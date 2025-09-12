package tools.vitruv.methodologisttemplate.vsum.domainSpecific;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

import brakesystem.BrakeCaliper;
import brakesystem.Brakesystem;
import brakesystem.BrakesystemFactory;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.methodologisttemplate.consistency.utils.StoexConsistencyHelper;
import tools.vitruv.methodologisttemplate.vsum.uncertainty.UncertaintyTestFactory;
import tools.vitruv.methodologisttemplate.vsum.uncertainty.UncertaintyTestUtil;
import tools.vitruv.stoex.interpreter.operations.MonteCarloOperation;
import tools.vitruv.stoex.stoex.SampledDistribution;
import uncertainty.Effect;
import uncertainty.StochasticityEffectType;
import uncertainty.StructuralEffectTypeRepresentation;
import uncertainty.Uncertainty;
import uncertainty.UncertaintyAnnotationRepository;
import uncertainty.UncertaintyFactory;
import uncertainty.UncertaintyKind;
import uncertainty.UncertaintyLocation;
import uncertainty.UncertaintyLocationType;

public class ClampingForceUncertaintyTest {

	private static final Logger logger = org.slf4j.LoggerFactory
			.getLogger(BrakeDiskDiameterUncertaintyTest.class);

	@BeforeAll
	static void setup() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*",
				new XMIResourceFactoryImpl());
	}

	@Test
	void deriveClampingForceTest(@TempDir Path tempDir) {

		VirtualModel vsum = UncertaintyTestUtil.createDefaultVirtualModel(tempDir);
		UncertaintyTestUtil.registerRootObjects(vsum, tempDir);

		CommittableView brakeSystemView = UncertaintyTestUtil.getDefaultView(vsum, List.of(Brakesystem.class))
				.withChangeRecordingTrait();
		modifyView(brakeSystemView, (CommittableView v) -> {
			var brakeCaliper = BrakesystemFactory.eINSTANCE.createBrakeCaliper();
			brakeCaliper.setPistonDiameterInMM(50);
			brakeCaliper.setHydraulicPressureInBar(80);
			v.getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents().add(brakeCaliper);
		});

		logger.debug("Added brake caliper with pistonDiameterInMM=50 and hydraulicPressureInBar=80");

		// ARRANGE + ACT
		// Add uncertainty to the pistonDiameterInMM and hydraulicPressureInBar
		modifyView(UncertaintyTestUtil.getDefaultView(vsum,
				List.of(UncertaintyAnnotationRepository.class, Brakesystem.class))
				.withChangeRecordingTrait(),
				(CommittableView v) -> {
					BrakeCaliper brakeCaliper = v.getRootObjects(Brakesystem.class).iterator()
							.next()
							.getBrakeComponents()
							.stream()
							.filter(BrakeCaliper.class::isInstance)
							.map(BrakeCaliper.class::cast)
							.filter(d -> d.getPistonDiameterInMM() == 50
									&& d.getHydraulicPressureInBar() == 80)
							.findFirst().orElseThrow();

					UncertaintyLocation pistonLocation = UncertaintyFactory.eINSTANCE
							.createUncertaintyLocation();
					pistonLocation.setLocation(UncertaintyLocationType.PARAMETER);
					pistonLocation.setSpecification("pistonDiameterInMM");
					pistonLocation.setParameter(
							brakeCaliper.eClass().getEStructuralFeature("pistonDiameterInMM"));
					pistonLocation.getReferencedComponents().add(brakeCaliper);

					UncertaintyLocation pressureLocation = UncertaintyFactory.eINSTANCE
							.createUncertaintyLocation();
					pressureLocation.setLocation(UncertaintyLocationType.PARAMETER);
					pressureLocation.setSpecification("hydraulicPressureInBar");
					pressureLocation.getReferencedComponents().addAll(List.of(brakeCaliper));

					// Setup Effect so that Reaction is triggered
					Effect pistonEffect = UncertaintyTestFactory.createEffect();
					pistonEffect.setRepresentation(StructuralEffectTypeRepresentation.CONTINOUS);
					pistonEffect.setStochasticity(StochasticityEffectType.PROBABILISTIC);
					pistonEffect.setSpecification("Normal(40, 2)");

					Effect pressureEffect = UncertaintyTestFactory.createEffect();
					pressureEffect.setRepresentation(StructuralEffectTypeRepresentation.CONTINOUS);
					pressureEffect.setStochasticity(StochasticityEffectType.PROBABILISTIC);
					pressureEffect.setSpecification("Sampled[70.0, 90.0, 75.0, 85.0, 80.0]");

					Uncertainty pistonUncertainty = UncertaintyTestFactory
							.createUncertainty(Optional.of(pistonLocation));
					pistonUncertainty.setKind(UncertaintyKind.MEASUREMENT_UNCERTAINTY);
					pistonUncertainty.setEffect(pistonEffect);

					Uncertainty pressureUncertainty = UncertaintyTestFactory
							.createUncertainty(Optional.of(pressureLocation));
					pressureUncertainty.setKind(UncertaintyKind.MEASUREMENT_UNCERTAINTY);
					pressureUncertainty.setEffect(pressureEffect);

					// Trigger propagation
					brakeCaliper.setSpecificationType("propagationTest");

					v.getRootObjects(UncertaintyAnnotationRepository.class).iterator().next()
							.getUncertainties().addAll(List.of(pistonUncertainty, pressureUncertainty));

				});

		// ASSERT
		// Check that another uncertainty was created
		// This uncertainty should be for the clampingForceInN parameter
		Assertions.assertTrue(assertView(UncertaintyTestUtil.getDefaultView(vsum,
				List.of(UncertaintyAnnotationRepository.class)), (View view) -> {
					Uncertainty clampingForceUncertainty = view
							.getRootObjects(UncertaintyAnnotationRepository.class)
							.iterator().next()
							.getUncertainties().stream()
							.filter(u -> u.getUncertaintyLocation()
									.getReferencedComponents().stream()
									.anyMatch(c -> c instanceof BrakeCaliper))
							.filter(u -> u.getUncertaintyLocation()
									.getLocation() == UncertaintyLocationType.PARAMETER)
							.filter(u -> u.getUncertaintyLocation().getSpecification()
									.equals("clampingForceInN"))
							.findFirst().orElseThrow();

					// Check the properties of the created uncertainty
					StoexConsistencyHelper helper = new StoexConsistencyHelper();
					String stoexExpression = clampingForceUncertainty.getEffect()
							.getSpecification();
					Object distribution = helper.evaluateToStoexExpression(stoexExpression);
					assertTrue(distribution instanceof SampledDistribution);
					SampledDistribution sampledDistribution = (SampledDistribution) distribution;
					MonteCarloOperation monteCarlo = new MonteCarloOperation();
					monteCarlo.printHistogram(
							sampledDistribution.getValues().stream().mapToDouble(Double::doubleValue).toArray(), 10);
					double mean = sampledDistribution.getValues().stream()
							.mapToDouble(Double::doubleValue)
							.average()
							.orElse(Double.NaN);
					assertEquals(15.707963270, mean, 100);
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
