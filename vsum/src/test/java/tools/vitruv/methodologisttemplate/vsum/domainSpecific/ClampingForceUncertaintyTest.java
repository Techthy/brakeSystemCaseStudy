package tools.vitruv.methodologisttemplate.vsum.domainSpecific;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
		// PI * (50 * 0.001/2)^2 * 78 * 10^2 = 15.31526419
		assertEquals(15.31526419, brakeCaliper.getClampingForceInN(), 0.1);
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
		// PI * (50 * 0.001/2)^2 * 78 * 10^2 = 15.31526419
		// Approximation error due to sampled distribution
		assertEquals(15.31526419, mean, 0.1);
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

		// Sampled distribution with mean 78
		SampledDistribution sampledDist = StoexFactory.eINSTANCE.createSampledDistribution();
		sampledDist.getValues()
				.addAll(List.of(60.0, 65.0, 70.0, 72.0, 74.0, 75.0, 76.0, 77.0, 78.0, 78.0, 78.0, 79.0, 80.0, 81.0,
						82.0, 84.0, 85.0, 86.0, 88.0, 92.0));
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

	private void modifyView(CommittableView view, Consumer<CommittableView> modificationFunction) {
		modificationFunction.accept(view);
		view.commitChanges();
	}

}
