package tools.vitruv.methodologisttemplate.consistency.utils;

import java.util.List;

import org.eclipse.emf.ecore.util.EcoreUtil;

import brakesystem.BrakeCaliper;
import tools.vitruv.stoex.stoex.Expression;
import uncertainty.Effect;
import uncertainty.Pattern;
import uncertainty.Uncertainty;
import uncertainty.UncertaintyAnnotationRepository;
import uncertainty.UncertaintyFactory;
import uncertainty.UncertaintyLocation;
import uncertainty.UncertaintyLocationType;
import uncertainty.UncertaintyPerspective;

public class ClampingForceHelper {

    /**
     * 
     * @param caliper
     */
    public static void recomputeClampingForce(BrakeCaliper caliper, UncertaintyAnnotationRepository uncertaintyRepo) {
        if (handleUncertaintyClampingForce(caliper, uncertaintyRepo)) {
            return;
        }
        Integer d = caliper.getPistonDiameterInMM();
        Double p = caliper.getHydraulicPressureInBar();
        double force = Math.PI * Math.pow(d * 0.001 / 2, 2) * p * Math.pow(10, 2);
        caliper.setClampingForceInN(force);
    }

    public static boolean handleUncertaintyClampingForce(BrakeCaliper caliper,
            UncertaintyAnnotationRepository uncertaintyRepo) {

        List<Uncertainty> uncertainties = uncertaintyRepo.getUncertainties().stream()
                .filter(u -> u.getUncertaintyLocation().getReferencedComponents().contains(caliper))
                .toList();

        if (uncertainties.isEmpty()) {
            return false;
        }

        Expression pistonDiameterExpr = getParameterUncertainty(uncertainties, caliper, "pistonDiameterInMM");
        Expression hydraulicPressureExpr = getParameterUncertainty(uncertainties, caliper, "hydraulicPressureInBar");

        StoexConsistencyHelper stoexHelper = new StoexConsistencyHelper();
        stoexHelper.putVariable("d", pistonDiameterExpr != null ? pistonDiameterExpr : caliper.getPistonDiameterInMM());
        stoexHelper.putVariable("p",
                hydraulicPressureExpr != null ? hydraulicPressureExpr : caliper.getHydraulicPressureInBar());

        String expr = "PI * ( (d * 0.001) / 2 ) ^ 2 * p * 10 ^ 2";
        Expression result = stoexHelper.evaluateToStoexExpression(expr);
        caliper.setClampingForceInN(stoexHelper.getMean(result).doubleValue());

        Uncertainty clampingForceUncertainty = copyUncertainty(uncertainties.get(0));
        clampingForceUncertainty.getUncertaintyLocation().getReferencedComponents().add(caliper);
        clampingForceUncertainty.getUncertaintyLocation().setParameterLocation("clampingForceInN");
        clampingForceUncertainty.getEffect().setExpression(result);
        uncertaintyRepo.getUncertainties().add(clampingForceUncertainty);

        return true;
    }

    private static Expression getParameterUncertainty(List<Uncertainty> uncertainties, BrakeCaliper caliper,
            String param) {
        return uncertainties.stream()
                .filter(u -> u.getUncertaintyLocation().getLocation() == UncertaintyLocationType.PARAMETER
                        && u.getUncertaintyLocation().getParameterLocation().equals(param)
                        && u.getEffect() != null && u.getEffect().getExpression() != null)
                .map(u -> u.getEffect().getExpression())
                .findFirst()
                .orElse(null);
    }

    private static Uncertainty copyUncertainty(Uncertainty original) {
        Uncertainty copy = uncertainty.UncertaintyFactory.eINSTANCE.createUncertainty();
        copy.setId(EcoreUtil.generateUUID());
        copy.setKind(original.getKind());
        copy.setNature(original.getNature());
        copy.setReducability(original.getReducability());
        // Deep copy of UncertaintyLocation
        UncertaintyLocation originalLocation = original.getUncertaintyLocation();
        UncertaintyLocation copyLocation = UncertaintyFactory.eINSTANCE.createUncertaintyLocation();
        copyLocation.setLocation(originalLocation.getLocation());
        copyLocation.setSpecification(originalLocation.getSpecification());
        copy.setUncertaintyLocation(copyLocation);
        // Deep copy of Effect
        Effect originalEffect = original.getEffect();
        Effect copyEffect = UncertaintyFactory.eINSTANCE.createEffect();
        copyEffect.setSpecification(originalEffect.getSpecification());
        copyEffect.setRepresentation(originalEffect.getRepresentation());
        copyEffect.setStochasticity(originalEffect.getStochasticity());
        copy.setEffect(copyEffect);
        // Deep copy of Pattern
        Pattern originalPattern = original.getPattern();
        Pattern copyPattern = UncertaintyFactory.eINSTANCE.createPattern();
        copyPattern.setPatternType(originalPattern.getPatternType());
        copy.setPattern(copyPattern);
        // Deep copy of UncertaintyPerspective
        UncertaintyPerspective originalPerspective = original.getPerspective();
        UncertaintyPerspective copyPerspective = UncertaintyFactory.eINSTANCE.createUncertaintyPerspective();
        copyPerspective.setPerspective(originalPerspective.getPerspective());
        copyPerspective.setSpecification(originalPerspective.getSpecification());
        copy.setPerspective(copyPerspective);
        copy.setOnDelete(original.getOnDelete());
        return copy;
    }

}
