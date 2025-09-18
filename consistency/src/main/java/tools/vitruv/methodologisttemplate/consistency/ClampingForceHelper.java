package tools.vitruv.methodologisttemplate.consistency;

import java.util.List;

import brakesystem.BrakeCaliper;
import tools.vitruv.methodologisttemplate.consistency.utils.StoexConsistencyHelper;
import tools.vitruv.stoex.stoex.Expression;
import uncertainty.Uncertainty;
import uncertainty.UncertaintyAnnotationRepository;
import uncertainty.UncertaintyLocationType;

public class ClampingForceHelper {

    /**
     * Recomputes the clamping force of the given caliper, taking into account any
     * uncertainties on piston diameter or hydraulic pressure. If uncertainties are
     * present, the clamping force is computed as an uncertainty expression and a
     * corresponding Uncertainty instance is created. If no uncertainties are
     * present, the clamping force is computed as a deterministic value.
     * 
     * @param caliper         the BrakeCaliper to update
     * @param uncertaintyRepo the UncertaintyAnnotationRepository containing
     *                        uncertainties
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

    /**
     * If uncertainties for piston diameter or hydraulic pressure exist for the
     * given caliper, computes the clamping force as an uncertainty expression and
     * creates a corresponding Uncertainty instance. Returns true if uncertainties
     * were found and handled, false otherwise.
     * 
     * @param caliper         the BrakeCaliper to update
     * @param uncertaintyRepo the UncertaintyAnnotationRepository containing
     *                        uncertainties
     * @return true if uncertainties were found and handled, false otherwise
     */
    private static boolean handleUncertaintyClampingForce(BrakeCaliper caliper,
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

        Uncertainty clampingForceUncertainty = UncertaintyReactionsHelper.deepCopyUncertainty(uncertainties.get(0));
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

}
