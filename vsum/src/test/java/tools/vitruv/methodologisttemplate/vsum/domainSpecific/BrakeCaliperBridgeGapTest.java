package tools.vitruv.methodologisttemplate.vsum.domainSpecific;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

import brakesystem.BrakeCaliper;
import brakesystem.BrakeDisk;
import brakesystem.Brakesystem;
import cad.CADRepository;
import cad.CShape;
import cad.Circle;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.methodologisttemplate.vsum.uncertainty.UncertaintyTestUtil;

/**
 * Test class to test the propagation of the bridge gap uncertainty from the CAD
 * model to the brake system model
 * 
 * Assumption: There is exactly one CShape in the CAD model that represents the
 * brake caliper
 * 
 * @author Claus Hammann
 *
 */
public class BrakeCaliperBridgeGapTest {

    private static final Logger logger = org.slf4j.LoggerFactory
            .getLogger(BrakeCaliperBridgeGapTest.class);

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*",
                new XMIResourceFactoryImpl());
    }

    @Test
    @DisplayName("Propagate Bridge Gap Uncertainty without Uncertainty")
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
        assertEquals(10, brakeDisk.getBrakeDiskThicknessInMM());

        /* Change Circle Extrusion (Brake Disk Thickness) */
        modifyView(CADview, this::changeCircleExtrusion);

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
        assertEquals(20, brakeDisk.getBrakeDiskThicknessInMM());

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

    private void changeCircleExtrusion(CommittableView view) {
        Circle circle = view.getRootObjects(CADRepository.class).iterator().next().getCadElements()
                .stream()
                .filter(Circle.class::isInstance)
                .map(Circle.class::cast)
                .findFirst().orElseThrow();
        circle.setExtrusion(20);
    }

    private void createCShapeAndCircle(CommittableView view) {
        Circle circle = cad.CadFactory.eINSTANCE.createCircle();
        circle.setExtrusion(10);
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
