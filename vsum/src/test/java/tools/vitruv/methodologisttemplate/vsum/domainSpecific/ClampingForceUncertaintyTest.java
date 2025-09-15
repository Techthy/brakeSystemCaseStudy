package tools.vitruv.methodologisttemplate.vsum.domainSpecific;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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
import tools.vitruv.stoex.stoex.NormalDistribution;
import tools.vitruv.stoex.stoex.SampledDistribution;
import tools.vitruv.stoex.stoex.StoexFactory;
import uncertainty.Uncertainty;
import uncertainty.UncertaintyAnnotationRepository;
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
	@DisplayName("Derive Clamping Force")
	void deriveClampingForceTest(@TempDir Path tempDir) {

		// SETUP VSUM
		VirtualModel vsum = UncertaintyTestUtil.createDefaultVirtualModel(tempDir);
		UncertaintyTestUtil.registerRootObjects(vsum, tempDir);

		CommittableView brakeSystemView = UncertaintyTestUtil.getDefaultView(vsum, List.of(Brakesystem.class))
				.withChangeRecordingTrait();
		modifyView(brakeSystemView, this::createBrakeCaliper);

		View brakeSystemAssertionView = UncertaintyTestUtil.getDefaultView(vsum,
				List.of(Brakesystem.class));
		BrakeCaliper brakeCaliper = getBrakeCaliperFromView(brakeSystemAssertionView);
		// PI * (50 * 0.001/2)^2 * 80 * 10^2 = 15.707963270
		assertEquals(15.707963270, brakeCaliper.getClampingForceInN(), 0.0001);
	}

	@Test
	@DisplayName("Derive Clamping Force with Uncertainty")
	void deriveClampingForceWithUncertaintyTest(@TempDir Path tempDir) {
		// SETUP VSUM
		VirtualModel vsum = UncertaintyTestUtil.createDefaultVirtualModel(tempDir);
		UncertaintyTestUtil.registerRootObjects(vsum, tempDir);

		CommittableView brakeSystemUncertaintyView = UncertaintyTestUtil
				.getDefaultView(vsum, List.of(Brakesystem.class, UncertaintyAnnotationRepository.class))
				.withChangeRecordingTrait();
		modifyView(brakeSystemUncertaintyView, this::createBrakeCaliperWithUncertainty);

		View brakeSystemAssertionView = UncertaintyTestUtil.getDefaultView(vsum,
				List.of(Brakesystem.class));
		BrakeCaliper brakeCaliper = getBrakeCaliperFromView(brakeSystemAssertionView);
		// PI * (50 * 0.001/2)^2 * 80 * 10^2 = 15.707963270
		assertEquals(15.707963270, brakeCaliper.getClampingForceInN(), 0.0001);

		View uncertaintyAssertionView = UncertaintyTestUtil.getDefaultView(vsum,
				List.of(UncertaintyAnnotationRepository.class));
		Uncertainty clampingForceUncertainty = uncertaintyAssertionView
				.getRootObjects(UncertaintyAnnotationRepository.class)
				.iterator().next()
				.getUncertainties().stream()
				.filter(u -> u.getUncertaintyLocation()
						.getReferencedComponents().stream()
						.anyMatch(c -> c instanceof BrakeCaliper))
				.filter(u -> u.getUncertaintyLocation()
						.getLocation() == UncertaintyLocationType.PARAMETER)
				.filter(u -> u.getUncertaintyLocation().getParameterLocation()
						.equals("clampingForceInN"))
				.findFirst().orElseThrow();
		StoexConsistencyHelper helper = new StoexConsistencyHelper();
		Double mean = helper.getMean(clampingForceUncertainty.getEffect().getExpression()).doubleValue();
		assertEquals(15.707963270, mean, 0.0001);

	}

	@Test
	@DisplayName("Derive Clamping Force with Uncertainty and Stoex Expression")
	void deriveClampingForceWithUncertaintyAndStoexExpressionTest(@TempDir Path tempDir) {
		// SETUP VSUM
		VirtualModel vsum = UncertaintyTestUtil.createDefaultVirtualModel(tempDir);
		UncertaintyTestUtil.registerRootObjects(vsum, tempDir);
		CommittableView brakeSystemUncertaintyView = UncertaintyTestUtil
				.getDefaultView(vsum, List.of(Brakesystem.class, UncertaintyAnnotationRepository.class))
				.withChangeRecordingTrait();
		modifyView(brakeSystemUncertaintyView, this::createBrakeCaliperWithUncertaintyAndStoexExpression);
		View brakeSystemAssertionView = UncertaintyTestUtil.getDefaultView(vsum,
				List.of(Brakesystem.class));
		BrakeCaliper brakeCaliper = getBrakeCaliperFromView(brakeSystemAssertionView);
		// PI * (50 * 0.001/2)^2 * 80 * 10^2 = 15.707963270
		assertEquals(15.707963270, brakeCaliper.getClampingForceInN(), 0.1);
		View uncertaintyAssertionView = UncertaintyTestUtil.getDefaultView(vsum,
				List.of(UncertaintyAnnotationRepository.class));
		Uncertainty clampingForceUncertainty = uncertaintyAssertionView
				.getRootObjects(UncertaintyAnnotationRepository.class)
				.iterator().next()
				.getUncertainties().stream()
				.filter(u -> u.getUncertaintyLocation()
						.getReferencedComponents().stream()
						.anyMatch(c -> c instanceof BrakeCaliper))
				.filter(u -> u.getUncertaintyLocation()
						.getLocation() == UncertaintyLocationType.PARAMETER)
				.filter(u -> u.getUncertaintyLocation().getParameterLocation()
						.equals("clampingForceInN"))
				.findFirst().orElseThrow();
		StoexConsistencyHelper helper = new StoexConsistencyHelper();
		Double mean = helper.getMean(clampingForceUncertainty.getEffect().getExpression()).doubleValue();
		// Approximation error due to sampled distribution
		assertEquals(15.707963270, mean, 0.1);
	}

	private void createBrakeCaliper(CommittableView view) {
		Brakesystem brakeSystem = view.getRootObjects(Brakesystem.class).iterator().next();
		BrakeCaliper brakeCaliper = BrakesystemFactory.eINSTANCE.createBrakeCaliper();
		brakeCaliper.setPistonDiameterInMM(50);
		brakeCaliper.setHydraulicPressureInBar(80);
		brakeSystem.getBrakeComponents().add(brakeCaliper);
	}

	private void createBrakeCaliperWithUncertainty(CommittableView view) {
		BrakeCaliper brakeCaliper = BrakesystemFactory.eINSTANCE.createBrakeCaliper();
		brakeCaliper.setPistonDiameterInMM(50);
		brakeCaliper.setHydraulicPressureInBar(80);
		view.getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents().add(brakeCaliper);

		UncertaintyLocation pistonLocation = UncertaintyTestFactory.createUncertaintyLocation(List.of(brakeCaliper));
		pistonLocation.setLocation(UncertaintyLocationType.PARAMETER);
		pistonLocation.setParameterLocation("pistonDiameterInMM");
		UncertaintyLocation pressureLocation = UncertaintyTestFactory.createUncertaintyLocation(List.of(brakeCaliper));
		pressureLocation.setParameterLocation("hydraulicPressureInBar");
		pressureLocation.setLocation(UncertaintyLocationType.PARAMETER);

		Uncertainty pistonUncertainty = UncertaintyTestFactory.createUncertainty(Optional.of(pistonLocation));
		Uncertainty pressureUncertainty = UncertaintyTestFactory.createUncertainty(Optional.of(pressureLocation));

		view.getRootObjects(UncertaintyAnnotationRepository.class).iterator().next()
				.getUncertainties().addAll(List.of(pistonUncertainty, pressureUncertainty));
	}

	private void createBrakeCaliperWithUncertaintyAndStoexExpression(CommittableView view) {
		BrakeCaliper brakeCaliper = BrakesystemFactory.eINSTANCE.createBrakeCaliper();
		brakeCaliper.setPistonDiameterInMM(50);
		brakeCaliper.setHydraulicPressureInBar(80);
		view.getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents().add(brakeCaliper);

		UncertaintyLocation pistonLocation = UncertaintyTestFactory.createUncertaintyLocation(List.of(brakeCaliper));
		pistonLocation.setLocation(UncertaintyLocationType.PARAMETER);
		pistonLocation.setParameterLocation("pistonDiameterInMM");
		UncertaintyLocation pressureLocation = UncertaintyTestFactory.createUncertaintyLocation(List.of(brakeCaliper));
		pressureLocation.setParameterLocation("hydraulicPressureInBar");
		pressureLocation.setLocation(UncertaintyLocationType.PARAMETER);

		Uncertainty pistonUncertainty = UncertaintyTestFactory.createUncertainty(Optional.of(pistonLocation));
		NormalDistribution dist = StoexFactory.eINSTANCE.createNormalDistribution();
		dist.setMu(50);
		dist.setSigma(2);
		pistonUncertainty.getEffect().setExpression(dist);

		SampledDistribution sampledDist = StoexFactory.eINSTANCE.createSampledDistribution();
		sampledDist.getValues().addAll(List.of(70.0, 90.0, 75.0, 85.0, 80.0));
		Uncertainty pressureUncertainty = UncertaintyTestFactory.createUncertainty(Optional.of(pressureLocation));
		pressureUncertainty.getEffect().setExpression(sampledDist);

		view.getRootObjects(UncertaintyAnnotationRepository.class).iterator().next()
				.getUncertainties().addAll(List.of(pistonUncertainty, pressureUncertainty));
	}

	private BrakeCaliper getBrakeCaliperFromView(View view) {
		return view.getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents().stream()
				.filter(BrakeCaliper.class::isInstance)
				.map(BrakeCaliper.class::cast)
				.findFirst().orElseThrow();
	}

	// @Test
	// void deriveClampingForceTestWithUncertainty(@TempDir Path tempDir) {

	// VirtualModel vsum = UncertaintyTestUtil.createDefaultVirtualModel(tempDir);
	// UncertaintyTestUtil.registerRootObjects(vsum, tempDir);

	// CommittableView brakeSystemView = UncertaintyTestUtil.getDefaultView(vsum,
	// List.of(Brakesystem.class))
	// .withChangeRecordingTrait();
	// modifyView(brakeSystemView, (CommittableView v) -> {
	// var brakeCaliper = BrakesystemFactory.eINSTANCE.createBrakeCaliper();
	// brakeCaliper.setPistonDiameterInMM(50);
	// brakeCaliper.setHydraulicPressureInBar(80);
	// v.getRootObjects(Brakesystem.class).iterator().next().getBrakeComponents().add(brakeCaliper);
	// });

	// logger.debug("Added brake caliper with pistonDiameterInMM=50 and
	// hydraulicPressureInBar=80");

	// // ARRANGE + ACT
	// // Add uncertainty to the pistonDiameterInMM and hydraulicPressureInBar
	// modifyView(UncertaintyTestUtil.getDefaultView(vsum,
	// List.of(UncertaintyAnnotationRepository.class, Brakesystem.class))
	// .withChangeRecordingTrait(),
	// (CommittableView v) -> {
	// BrakeCaliper brakeCaliper = v.getRootObjects(Brakesystem.class).iterator()
	// .next()
	// .getBrakeComponents()
	// .stream()
	// .filter(BrakeCaliper.class::isInstance)
	// .map(BrakeCaliper.class::cast)
	// .filter(d -> d.getPistonDiameterInMM() == 50
	// && d.getHydraulicPressureInBar() == 80)
	// .findFirst().orElseThrow();

	// UncertaintyLocation pistonLocation = UncertaintyFactory.eINSTANCE
	// .createUncertaintyLocation();
	// pistonLocation.setLocation(UncertaintyLocationType.PARAMETER);
	// pistonLocation.setSpecification("pistonDiameterInMM");
	// // pistonLocation.setParameter(
	// // brakeCaliper.eClass().getEStructuralFeature("pistonDiameterInMM"));
	// pistonLocation.getReferencedComponents().add(brakeCaliper);

	// UncertaintyLocation pressureLocation = UncertaintyFactory.eINSTANCE
	// .createUncertaintyLocation();
	// pressureLocation.setLocation(UncertaintyLocationType.PARAMETER);
	// pressureLocation.setSpecification("hydraulicPressureInBar");
	// pressureLocation.getReferencedComponents().addAll(List.of(brakeCaliper));

	// // Setup Effect so that Reaction is triggered
	// Effect pistonEffect = UncertaintyTestFactory.createEffect();
	// pistonEffect.setRepresentation(StructuralEffectTypeRepresentation.CONTINOUS);
	// pistonEffect.setStochasticity(StochasticityEffectType.PROBABILISTIC);
	// pistonEffect.setSpecification("Normal(40, 2)");

	// Effect pressureEffect = UncertaintyTestFactory.createEffect();
	// pressureEffect.setRepresentation(StructuralEffectTypeRepresentation.CONTINOUS);
	// pressureEffect.setStochasticity(StochasticityEffectType.PROBABILISTIC);
	// pressureEffect.setSpecification("Sampled[70.0, 90.0, 75.0, 85.0, 80.0]");

	// Uncertainty pistonUncertainty = UncertaintyTestFactory
	// .createUncertainty(Optional.of(pistonLocation));
	// pistonUncertainty.setKind(UncertaintyKind.MEASUREMENT_UNCERTAINTY);
	// pistonUncertainty.setEffect(pistonEffect);

	// Uncertainty pressureUncertainty = UncertaintyTestFactory
	// .createUncertainty(Optional.of(pressureLocation));
	// pressureUncertainty.setKind(UncertaintyKind.MEASUREMENT_UNCERTAINTY);
	// pressureUncertainty.setEffect(pressureEffect);

	// // Trigger propagation
	// brakeCaliper.setSpecificationType("propagationTest");

	// v.getRootObjects(UncertaintyAnnotationRepository.class).iterator().next()
	// .getUncertainties().addAll(List.of(pistonUncertainty, pressureUncertainty));

	// });

	// // ASSERT
	// // Check that another uncertainty was created
	// // This uncertainty should be for the clampingForceInN parameter
	// Assertions.assertTrue(assertView(UncertaintyTestUtil.getDefaultView(vsum,
	// List.of(UncertaintyAnnotationRepository.class)), (View view) -> {
	// Uncertainty clampingForceUncertainty = view
	// .getRootObjects(UncertaintyAnnotationRepository.class)
	// .iterator().next()
	// .getUncertainties().stream()
	// .filter(u -> u.getUncertaintyLocation()
	// .getReferencedComponents().stream()
	// .anyMatch(c -> c instanceof BrakeCaliper))
	// .filter(u -> u.getUncertaintyLocation()
	// .getLocation() == UncertaintyLocationType.PARAMETER)
	// .filter(u -> u.getUncertaintyLocation().getSpecification()
	// .equals("clampingForceInN"))
	// .findFirst().orElseThrow();

	// // Check the properties of the created uncertainty
	// StoexConsistencyHelper helper = new StoexConsistencyHelper();
	// String stoexExpression = clampingForceUncertainty.getEffect()
	// .getSpecification();
	// Object distribution = helper.evaluateToStoexExpression(stoexExpression);
	// assertTrue(distribution instanceof SampledDistribution);
	// SampledDistribution sampledDistribution = (SampledDistribution) distribution;
	// MonteCarloOperation monteCarlo = new MonteCarloOperation();
	// monteCarlo.printHistogram(
	// sampledDistribution.getValues().stream().mapToDouble(Double::doubleValue).toArray(),
	// 10);
	// double mean = sampledDistribution.getValues().stream()
	// .mapToDouble(Double::doubleValue)
	// .average()
	// .orElse(Double.NaN);
	// assertEquals(15.707963270, mean, 0.0001);
	// return true;
	// }));

	// }

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
