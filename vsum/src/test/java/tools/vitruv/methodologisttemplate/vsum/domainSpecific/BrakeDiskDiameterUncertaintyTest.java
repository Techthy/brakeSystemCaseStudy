package tools.vitruv.methodologisttemplate.vsum.domainSpecific;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

import brakesystem.BrakeDisk;
import brakesystem.Brakesystem;
import cad.CADRepository;
import cad.Circle;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.methodologisttemplate.vsum.uncertainty.UncertaintyTestFactory;
import tools.vitruv.methodologisttemplate.vsum.uncertainty.UncertaintyTestUtil;
import uncertainty.Effect;
import uncertainty.StochasticityEffectType;
import uncertainty.StructuralEffectTypeRepresentation;
import uncertainty.Uncertainty;
import uncertainty.UncertaintyAnnotationRepository;
import uncertainty.UncertaintyKind;
import uncertainty.UncertaintyLocation;

public class BrakeDiskDiameterUncertaintyTest {
	private static final Logger logger = org.slf4j.LoggerFactory
			.getLogger(BrakeDiskDiameterUncertaintyTest.class);

	@BeforeAll
	static void setup() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
	}

	@Test
	void updateBrakeDiskDiameterWithUncetaintyTest(@TempDir Path tempDir) {

		// Setup - one Brake Disk with an uncertainty that is propagated to the CAD
		// Circle
		VirtualModel vsum = UncertaintyTestUtil.createDefaultVirtualModel(tempDir);
		UncertaintyTestUtil.registerRootObjects(vsum, tempDir);

		UncertaintyTestUtil.addBrakeDiscWithDiameter(vsum, tempDir, 200);

		assertDiskAndCircleArePresent(vsum);

		modifyView(UncertaintyTestUtil.getDefaultView(vsum,
				List.of(UncertaintyAnnotationRepository.class, Brakesystem.class))
				.withChangeRecordingTrait(),
				(CommittableView v) -> {
					BrakeDisk brakeDisk = v.getRootObjects(Brakesystem.class).iterator().next()
							.getBrakeComponents()
							.stream()
							.filter(BrakeDisk.class::isInstance).map(BrakeDisk.class::cast)
							.filter(d -> d.getDiameterInMM() == 200)
							.findFirst().orElseThrow();

					UncertaintyLocation uncertaintyLocation = UncertaintyTestFactory
							.createUncertaintyLocation(List.of(brakeDisk));

					// Setup Effect so that Reaction is triggered
					Effect uncertaintyEffect = UncertaintyTestFactory.createEffect();
					uncertaintyEffect.setRepresentation(StructuralEffectTypeRepresentation.CONTINOUS);
					uncertaintyEffect.setStochasticity(StochasticityEffectType.PROBABILISTIC);

					Uncertainty uncertainty = UncertaintyTestFactory
							.createUncertainty(Optional.of(uncertaintyLocation));
					uncertainty.setKind(UncertaintyKind.BELIEF_UNCERTAINTY);
					uncertainty.setEffect(uncertaintyEffect);

					// Trigger propagation
					brakeDisk.setSpecificationType("propagationTest");

					v.getRootObjects(UncertaintyAnnotationRepository.class).iterator().next()
							.getUncertainties().add(uncertainty);

				});

		// Let the uncertainty propagate
		// Then change the effect specification to test if the custom rule applies

		logger.info("Modifying the effect specification to Normal(196.0, 3.0) for the brake disk");

		modifyView(UncertaintyTestUtil.getDefaultView(vsum,
				List.of(UncertaintyAnnotationRepository.class, Brakesystem.class))
				.withChangeRecordingTrait(),
				(CommittableView v) -> {
					// Get Uncertainty belonging to the brake disk
					Uncertainty uncertainty = v.getRootObjects(UncertaintyAnnotationRepository.class).iterator().next()
							.getUncertainties().stream()
							.filter(u -> {
								UncertaintyLocation location = u.getUncertaintyLocation();
								return location != null &&
										location.getReferencedComponents().stream()
												.anyMatch(BrakeDisk.class::isInstance);
							})
							.findFirst()
							.orElseThrow(() -> new AssertionError("Expected uncertainty not found"));

					// set effect specification (meaning that the diameter is uncertain and normal
					// distributed like so:)
					uncertainty.getEffect().setSpecification("Normal(196.0, 3.0)");
				});

		Assertions.assertTrue(assertView(UncertaintyTestUtil.getDefaultView(vsum,
				List.of(UncertaintyAnnotationRepository.class)), (View view) -> {
					Uncertainty uncertainty = view.getRootObjects(UncertaintyAnnotationRepository.class).iterator()
							.next()
							.getUncertainties()
							.stream()
							.filter(u -> {
								UncertaintyLocation location = u.getUncertaintyLocation();
								return location != null &&
										location.getReferencedComponents().stream().anyMatch(Circle.class::isInstance);
							})
							.findFirst()
							.orElseThrow(() -> new AssertionError("Expected uncertainty not found"));

					// the normal distribution gets added to another one specifying Normal(0, 4)
					// therefore following the math the specification for the circle should be
					Assertions.assertEquals("Normal(196.0,5.0)",
							uncertainty.getEffect().getSpecification().replace(" ", ""));
					return true;
				}));

	}

	private void assertDiskAndCircleArePresent(VirtualModel vsum) {
		// Assert that brake disk with diameter 200 and circle with radius 100 are
		// present
		Assertions.assertTrue(assertView(UncertaintyTestUtil.getDefaultView(vsum,
				List.of(CADRepository.class, Brakesystem.class)), (View view) -> {
					boolean circlePresent = view.getRootObjects(CADRepository.class).iterator()
							.next().getCadElements()
							.stream()
							.filter(Circle.class::isInstance).map(Circle.class::cast)
							.filter(d -> d.getRadius() == 100)
							.findFirst().isPresent();
					logger.debug("Circle present: " + circlePresent);
					boolean brakeDiskPresent = view.getRootObjects(Brakesystem.class).iterator()
							.next()
							.getBrakeComponents()
							.stream()
							.filter(BrakeDisk.class::isInstance).map(BrakeDisk.class::cast)
							.filter(d -> d.getDiameterInMM() == 200)
							.findFirst().isPresent();
					logger.debug("BrakeDisk present: " + brakeDiskPresent);
					return circlePresent;
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
