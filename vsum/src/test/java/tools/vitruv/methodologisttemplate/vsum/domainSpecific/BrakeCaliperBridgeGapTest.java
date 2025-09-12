package tools.vitruv.methodologisttemplate.vsum.domainSpecific;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

import brakesystem.BrakeCaliper;
import brakesystem.Brakesystem;
import cad.CADRepository;
import cad.Circle;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.methodologisttemplate.vsum.uncertainty.UncertaintyTestUtil;

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

        VirtualModel vsum = UncertaintyTestUtil.createDefaultVirtualModel(tempDir);
        UncertaintyTestUtil.registerRootObjects(vsum, tempDir);

        // ARRANGE + ACT
        CommittableView CADview = UncertaintyTestUtil.getDefaultView(vsum, List.of(CADRepository.class))
                .withChangeRecordingTrait();
        modifyView(CADview, (CommittableView v) -> {
            var cShape = cad.CadFactory.eINSTANCE.createCShape();
            cShape.setThroatWidth(42);
            v.getRootObjects(CADRepository.class).iterator().next().getCadElements().add(cShape);
            Circle circle = cad.CadFactory.eINSTANCE.createCircle();
            circle.setExtrusion(0);
            circle.setRadius(100);
            v.getRootObjects(CADRepository.class).iterator().next().getCadElements().add(circle);
        });
        logger.debug("Added CShape with throatWidth=42");

        // ASSERT
        // Check that a brake caliper was created with the correct bridge gap in the
        // brake system
        Assertions.assertTrue(assertView(UncertaintyTestUtil.getDefaultView(vsum,
                List.of(Brakesystem.class)), (View view) -> {
                    BrakeCaliper brakeCaliper = view
                            .getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents().stream()
                            .filter(BrakeCaliper.class::isInstance)
                            .map(BrakeCaliper.class::cast)
                            .findFirst().orElseThrow();
                    assertEquals(42, brakeCaliper.getBridgeGap());
                    return true;
                }));

        // ACT
        // Change the extrusion of the circle
        modifyView(CADview, (CommittableView v) -> {
            Circle circle = v.getRootObjects(CADRepository.class).iterator().next().getCadElements()
                    .stream()
                    .filter(Circle.class::isInstance)
                    .map(Circle.class::cast)
                    .findFirst().orElseThrow();
            circle.setExtrusion(20);
        });

        // ASSERT
        // Check that the brake caliper was updated with the correct bridge gap in the
        // brake system
        Assertions.assertTrue(assertView(UncertaintyTestUtil.getDefaultView(vsum,
                List.of(Brakesystem.class)), (View view) -> {
                    BrakeCaliper brakeCaliper = view
                            .getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents().stream()
                            .filter(BrakeCaliper.class::isInstance)
                            .map(BrakeCaliper.class::cast)
                            .findFirst().orElseThrow();
                    assertEquals(62, brakeCaliper.getBridgeGap());
                    return true;
                }));

    }

    // These functions are only for convience, as they make the code a bit better
    // readable
    private void modifyView(CommittableView view, Consumer<CommittableView> modificationFunction) {
        modificationFunction.accept(view);
        view.commitChanges();
    }

    private boolean assertView(View view, Function<View, Boolean> viewAssertionFunction) {
        return viewAssertionFunction.apply(view);
    }
}
