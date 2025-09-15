package tools.vitruv.methodologisttemplate.vsum.domainSpecific;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import brakesystem.BrakeCaliper;
import brakesystem.BrakeDisk;
import brakesystem.Brakesystem;
import cad.CADRepository;
import cad.CShape;
import cad.Circle;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.methodologisttemplate.vsum.uncertainty.UncertaintyTestFactory;
import tools.vitruv.methodologisttemplate.vsum.uncertainty.UncertaintyTestUtil;
import tools.vitruv.stoex.stoex.NormalDistribution;
import tools.vitruv.stoex.stoex.StoexFactory;
import uncertainty.Uncertainty;
import uncertainty.UncertaintyAnnotationRepository;
import uncertainty.UncertaintyKind;
import uncertainty.UncertaintyLocation;
import uncertainty.UncertaintyLocationType;

/**
 * This test class tests the propagation of the bridge gap and its dependence on
 * the brake disk thickness
 * The first test, shows the propagation without using the UnCertaGator.
 * The second test shows the same propagation but with the UnCertaGator and
 * uncertainty annotations.
 * The third test shows the same propagation now using the UnCertaGator with the
 * StoEx extension.
 * 
 * Note: The tests as well as the reactions corresponding to the test are always
 * assuming that there is exactly one
 * brake caliper and one brake disk in the brake system and exactly one CShape
 * and one Circle in the CAD model.
 *
 * @author Claus Hammann
 */
public class BrakeCaliperBridgeGapTest {

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*",
                new XMIResourceFactoryImpl());
    }

    @Test
    @DisplayName("Propagate Bridge Gap without Uncertainty")
    void propagateBridgeGapTest(@TempDir Path tempDir) {

        // SETUP VSUM
        VirtualModel vsum = UncertaintyTestUtil.createDefaultVirtualModel(tempDir);
        UncertaintyTestUtil.registerRootObjects(vsum, tempDir);

        /**
         * Create CShape and Circle representing the brake caliper and disk
         * The CShape has a throatWidth of 42, which should be propagated to the
         * brake caliper as bridgeGap
         * The Circle has an extrusion of 10, which should also be propagated to the
         * the brake system as the disks thickness
         */
        CommittableView CADview = UncertaintyTestUtil.getDefaultView(vsum, List.of(CADRepository.class))
                .withChangeRecordingTrait();
        modifyView(CADview, this::createCShapeAndCircle);

        /**
         * Assert that the brake caliper and disk were created in the brake system with
         * a bridgeGap of 42 and a thickness of 10
         */
        View brakeSystemView = UncertaintyTestUtil.getDefaultView(vsum,
                List.of(Brakesystem.class));
        BrakeCaliper brakeCaliper = getBrakeCaliperFromView(brakeSystemView);
        assertEquals(42, brakeCaliper.getBridgeGap());
        BrakeDisk brakeDisk = getBrakeDiskFromView(brakeSystemView);
        assertEquals(20, brakeDisk.getBrakeDiskThicknessInMM());

        /* Change the Brake Disk Thickness (should propagate to CAD) */
        CommittableView brakeSysCommittableView = UncertaintyTestUtil.getDefaultView(vsum,
                List.of(Brakesystem.class)).withChangeRecordingTrait();
        modifyView(brakeSysCommittableView, this::changeBrakeDiskThickness);

        /**
         * Assert that the throat width was automatically recalculated
         * to 52 (42 + (20 - 10)) in the CAD model
         * Assert that the bridge gap was then updated to 52 in the brake system
         * Assert that the brake disk was updated with a thickness of 20 in the brake
         * system
         */
        View updatedCADView = UncertaintyTestUtil.getDefaultView(vsum, List.of(CADRepository.class));
        CShape cShape = getCShapeFromView(updatedCADView);
        assertEquals(52, cShape.getThroatWidth());
        View updatedBrakeSystemView = UncertaintyTestUtil.getDefaultView(vsum, List.of(Brakesystem.class));
        brakeCaliper = getBrakeCaliperFromView(updatedBrakeSystemView);
        assertEquals(52, brakeCaliper.getBridgeGap());
        brakeDisk = getBrakeDiskFromView(updatedBrakeSystemView);
        assertEquals(30, brakeDisk.getBrakeDiskThicknessInMM());

    }

    @Test
    @DisplayName("Propagate Bridge Gap with Uncertainty")
    void propagateBridgeGapWithUncertainty(@TempDir Path tempDir) {

        // SETUP VSUM
        VirtualModel vsum = UncertaintyTestUtil.createDefaultVirtualModel(tempDir);
        UncertaintyTestUtil.registerRootObjects(vsum, tempDir);

        /**
         * Again create CShape and Circle as above
         */
        CommittableView CADview = UncertaintyTestUtil.getDefaultView(vsum, List.of(CADRepository.class))
                .withChangeRecordingTrait();
        modifyView(CADview, this::createCShapeAndCircle);
        /**
         * Assert that the brake caliper and disk were created in the brake system with
         * a bridgeGap of 42 and a thickness of 10
         */
        View brakeSystemView = UncertaintyTestUtil.getDefaultView(vsum,
                List.of(Brakesystem.class));
        BrakeCaliper brakeCaliper = getBrakeCaliperFromView(brakeSystemView);
        assertEquals(42, brakeCaliper.getBridgeGap());
        BrakeDisk brakeDisk = getBrakeDiskFromView(brakeSystemView);
        assertEquals(20, brakeDisk.getBrakeDiskThicknessInMM());

        /* Change the Brake Disk Thickness this time annotated with uncertainty */
        CommittableView brakeSysCommittableView = UncertaintyTestUtil.getDefaultView(vsum,
                List.of(Brakesystem.class, UncertaintyAnnotationRepository.class))
                .withChangeRecordingTrait();
        modifyView(brakeSysCommittableView, this::changeBrakeDiskThicknessWithUncertainty);

        /**
         * Assert that the throat width was automatically recalculated
         * to 52 (42 + (20 - 10)) in the CAD model
         * Assert that the bridge gap was then updated to 52 in the brake system
         * Assert that the brake disk was updated with a thickness of 20 in the brake
         * system
         * Assert that there is an uncertainty for the brake disk thickness in the brake
         * system
         * Assert that there is an uncertainty for the circle's extrusion in the CAD
         * model
         */
        // Same as before
        View updatedCADView = UncertaintyTestUtil.getDefaultView(vsum, List.of(CADRepository.class));
        CShape cShape = getCShapeFromView(updatedCADView);
        assertEquals(52, cShape.getThroatWidth());
        View updatedBrakeSystemView = UncertaintyTestUtil.getDefaultView(vsum, List.of(Brakesystem.class));
        brakeCaliper = getBrakeCaliperFromView(updatedBrakeSystemView);
        assertEquals(52, brakeCaliper.getBridgeGap());
        brakeDisk = getBrakeDiskFromView(updatedBrakeSystemView);
        assertEquals(30, brakeDisk.getBrakeDiskThicknessInMM());
        assertEquals(52, cShape.getThroatWidth());
        // Now check that uncertainties are present
        // (the helper function already check some of the properties)
        View uncertaintyView = UncertaintyTestUtil.getDefaultView(vsum,
                List.of(Brakesystem.class, CADRepository.class, UncertaintyAnnotationRepository.class));
        Uncertainty brakeDiskThicknessUncertainty = getBrakeDiskThicknessUncertainty(uncertaintyView);
        assertEquals(UncertaintyKind.MEASUREMENT_UNCERTAINTY, brakeDiskThicknessUncertainty.getKind());
        Uncertainty circleExtrusionUncertainty = getCircleExtrusionUncertainty(uncertaintyView);
        assertEquals(UncertaintyKind.MEASUREMENT_UNCERTAINTY, circleExtrusionUncertainty.getKind());
    }

    @Test
    @DisplayName("Propagate Bridge Gap with Uncertainty and StoEx annotation")
    void propagateBridgeGapWithUncertaintyAndStoExTest(@TempDir Path tempDir) {

        // SETUP VSUM
        VirtualModel vsum = UncertaintyTestUtil.createDefaultVirtualModel(tempDir);
        UncertaintyTestUtil.registerRootObjects(vsum, tempDir);

        /**
         * Again create CShape and Circle as above
         */
        CommittableView CADview = UncertaintyTestUtil.getDefaultView(vsum, List.of(CADRepository.class))
                .withChangeRecordingTrait();
        modifyView(CADview, this::createCShapeAndCircle);
        /**
         * Assert that the brake caliper and disk were created in the brake system with
         * a bridgeGap of 42 and a thickness of 10
         */
        View brakeSystemView = UncertaintyTestUtil.getDefaultView(vsum,
                List.of(Brakesystem.class));
        BrakeCaliper brakeCaliper = getBrakeCaliperFromView(brakeSystemView);
        assertEquals(42, brakeCaliper.getBridgeGap());
        BrakeDisk brakeDisk = getBrakeDiskFromView(brakeSystemView);
        assertEquals(20, brakeDisk.getBrakeDiskThicknessInMM());

        /* Change the Brake Disk Thickness this time annotated with uncertainty */
        CommittableView brakeSysCommittableView = UncertaintyTestUtil.getDefaultView(vsum,
                List.of(Brakesystem.class, UncertaintyAnnotationRepository.class))
                .withChangeRecordingTrait();
        modifyView(brakeSysCommittableView, this::changeBrakeDiskThicknessWithUncertaintyAndStoEx);
        /* Let the uncertainty propagate -> corresponding uncertainties are present */
        /* Now add a StoEx annotation to the uncertainty of the brake disk thickness */
        CommittableView brakeSysCommittableViewWithUncertainties = UncertaintyTestUtil.getDefaultView(vsum,
                List.of(Brakesystem.class, UncertaintyAnnotationRepository.class))
                .withChangeRecordingTrait();
        modifyView(brakeSysCommittableViewWithUncertainties, this::addStoexAnnotationToBrakeDisk);

        /**
         * Assert that the throat width was automatically recalculated
         * to 52 (42 + (20 - 10)) in the CAD model
         * Assert that the bridge gap was then updated to 52 in the brake system
         * Assert that the brake disk was updated with a thickness of 20 in the brake
         * system
         * Assert that there is an uncertainty for the brake disk thickness in the brake
         * system
         * Assert that there is an uncertainty for the circle's extrusion in the CAD
         * model
         */
        // Same as before
        View updatedCADView = UncertaintyTestUtil.getDefaultView(vsum, List.of(CADRepository.class));
        CShape cShape = getCShapeFromView(updatedCADView);
        assertEquals(52, cShape.getThroatWidth());
        View updatedBrakeSystemView = UncertaintyTestUtil.getDefaultView(vsum, List.of(Brakesystem.class));
        brakeCaliper = getBrakeCaliperFromView(updatedBrakeSystemView);
        assertEquals(52, brakeCaliper.getBridgeGap());
        brakeDisk = getBrakeDiskFromView(updatedBrakeSystemView);
        assertEquals(30, brakeDisk.getBrakeDiskThicknessInMM());
        assertEquals(52, cShape.getThroatWidth());
        // Now check that uncertainties are present
        // (the helper function already check some of the properties)
        View uncertaintyView = UncertaintyTestUtil.getDefaultView(vsum,
                List.of(Brakesystem.class, CADRepository.class, UncertaintyAnnotationRepository.class));
        Uncertainty brakeDiskThicknessUncertainty = getBrakeDiskThicknessUncertainty(uncertaintyView);
        assertEquals(UncertaintyKind.MEASUREMENT_UNCERTAINTY, brakeDiskThicknessUncertainty.getKind());
        // Check that the first propagation (to the brake Circle has the correct StoEx)
        Uncertainty circleExtrusionUncertainty = getCircleExtrusionUncertainty(uncertaintyView);
        assertEquals(UncertaintyKind.MEASUREMENT_UNCERTAINTY, circleExtrusionUncertainty.getKind());
        assertEquals(30, ((NormalDistribution) circleExtrusionUncertainty.getEffect().getExpression()).getMu());
        assertEquals(0.667, ((NormalDistribution) circleExtrusionUncertainty.getEffect().getExpression())
                .getSigma());
        Uncertainty cShapeThroatWidthUncertainty = getCShapeThroatWidthUncertainty(uncertaintyView);
        assertEquals(UncertaintyKind.MEASUREMENT_UNCERTAINTY, cShapeThroatWidthUncertainty.getKind());
        // Now check that the expression is correct
        // It should be a StoEx expression representing the following formula:
        // throatWidth + (30 - 10)
        assertTrue(cShapeThroatWidthUncertainty.getEffect().getExpression() instanceof NormalDistribution);
        assertEquals(52, ((NormalDistribution) cShapeThroatWidthUncertainty.getEffect().getExpression())
                .getMu());
        assertEquals(0.667, ((NormalDistribution) cShapeThroatWidthUncertainty.getEffect().getExpression())
                .getSigma());
    }

    // Helper functions to get the relevant elements from the views

    private CShape getCShapeFromView(View view) {
        return view.getRootObjects(CADRepository.class).iterator().next().getCadElements().stream()
                .filter(CShape.class::isInstance)
                .map(CShape.class::cast)
                .findFirst().orElseThrow();
    }

    private BrakeCaliper getBrakeCaliperFromView(View view) {
        return view.getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents().stream()
                .filter(BrakeCaliper.class::isInstance)
                .map(BrakeCaliper.class::cast)
                .findFirst().orElseThrow();
    }

    private BrakeDisk getBrakeDiskFromView(View view) {
        return view.getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents().stream()
                .filter(BrakeDisk.class::isInstance)
                .map(BrakeDisk.class::cast)
                .findFirst().orElseThrow();
    }

    private void changeBrakeDiskThickness(CommittableView view) {
        BrakeDisk brakeDisk = view.getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents()
                .stream()
                .filter(BrakeDisk.class::isInstance)
                .map(BrakeDisk.class::cast)
                .findFirst().orElseThrow();
        brakeDisk.setBrakeDiskThicknessInMM(30);
    }

    private void changeBrakeDiskThicknessWithUncertainty(CommittableView view) {
        changeBrakeDiskThickness(view);

        // Create uncertainty for the brake disk thickness
        UncertaintyLocation location = UncertaintyTestFactory.createUncertaintyLocation(
                List.of(getBrakeDiskFromView(view)), UncertaintyLocationType.PARAMETER, "brakeDiskThicknessInMM");
        Uncertainty uncertainty = UncertaintyTestFactory.createUncertainty(Optional.of(location));
        uncertainty.setKind(UncertaintyKind.MEASUREMENT_UNCERTAINTY);
        view.getRootObjects(UncertaintyAnnotationRepository.class).iterator().next()
                .getUncertainties().add(uncertainty);
    }

    private void changeBrakeDiskThicknessWithUncertaintyAndStoEx(CommittableView view) {
        // int expectedThickness = 20;
        BrakeDisk brakeDisk = view.getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents()
                .stream()
                .filter(BrakeDisk.class::isInstance)
                .map(BrakeDisk.class::cast)
                .findFirst().orElseThrow();
        // brakeDisk.setBrakeDiskThicknessInMM(expectedThickness);

        // Create uncertainty for the brake disk thickness
        UncertaintyLocation location = UncertaintyTestFactory.createUncertaintyLocation(
                List.of(brakeDisk), UncertaintyLocationType.PARAMETER, "brakeDiskThicknessInMM");
        Uncertainty uncertainty = UncertaintyTestFactory.createUncertainty(Optional.of(location));
        uncertainty.setKind(UncertaintyKind.MEASUREMENT_UNCERTAINTY);
        view.getRootObjects(UncertaintyAnnotationRepository.class).iterator().next()
                .getUncertainties().add(uncertainty);
    }

    private void addStoexAnnotationToBrakeDisk(CommittableView view) {
        BrakeDisk brakeDisk = view.getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents()
                .stream()
                .filter(BrakeDisk.class::isInstance)
                .map(BrakeDisk.class::cast)
                .findFirst().orElseThrow();
        brakeDisk.setBrakeDiskThicknessInMM(30);

        Uncertainty brakeDiskUncertainty = getBrakeDiskThicknessUncertainty(view);
        NormalDistribution normalDist = StoexFactory.eINSTANCE.createNormalDistribution();
        normalDist.setMu(30);
        normalDist.setSigma(0.667);
        brakeDiskUncertainty.getEffect().setExpression(normalDist);
    }

    private Uncertainty getBrakeDiskThicknessUncertainty(View view) {
        return view.getRootObjects(UncertaintyAnnotationRepository.class).iterator().next()
                .getUncertainties().stream()
                .filter(u -> u.getUncertaintyLocation()
                        .getLocation() == UncertaintyLocationType.PARAMETER
                        && u.getUncertaintyLocation().getParameterLocation()
                                .equals("brakeDiskThicknessInMM")
                        && u.getUncertaintyLocation().getReferencedComponents().stream()
                                .anyMatch(e -> e instanceof BrakeDisk))
                .findFirst().orElseThrow();
    }

    private Uncertainty getCircleExtrusionUncertainty(View view) {
        return view.getRootObjects(UncertaintyAnnotationRepository.class).iterator().next()
                .getUncertainties().stream()
                .filter(u -> u.getUncertaintyLocation()
                        .getLocation() == UncertaintyLocationType.PARAMETER
                        // &&
                        // u.getUncertaintyLocation().getParameterLocation().equals("extrusion")
                        && u.getUncertaintyLocation().getReferencedComponents().stream()
                                .anyMatch(e -> e instanceof Circle))
                .findFirst().orElseThrow();
    }

    private Uncertainty getCShapeThroatWidthUncertainty(View view) {
        return view.getRootObjects(UncertaintyAnnotationRepository.class).iterator().next()
                .getUncertainties().stream()
                .filter(u -> u.getUncertaintyLocation()
                        .getLocation() == UncertaintyLocationType.PARAMETER
                        && u.getUncertaintyLocation().getParameterLocation() != null
                        && u.getUncertaintyLocation().getParameterLocation()
                                .equals("throatWidth")
                        && u.getUncertaintyLocation().getReferencedComponents().stream()
                                .anyMatch(e -> e instanceof CShape))
                .findFirst().orElseThrow();
    }

    private void createCShapeAndCircle(CommittableView view) {
        Circle circle = cad.CadFactory.eINSTANCE.createCircle();
        circle.setExtrusion(20);
        circle.setRadius(100);
        circle.setIdentifier("BrakeDiskCircle");
        view.getRootObjects(CADRepository.class).iterator().next().getCadElements().add(circle);

        CShape cShape = cad.CadFactory.eINSTANCE.createCShape();
        cShape.setThroatWidth(42);
        cShape.setIdentifier("BrakeCaliperCShape");
        view.getRootObjects(CADRepository.class).iterator().next().getCadElements().add(cShape);
    }

    private void modifyView(CommittableView view, Consumer<CommittableView> modificationFunction) {
        modificationFunction.accept(view);
        view.commitChanges();
    }

}
