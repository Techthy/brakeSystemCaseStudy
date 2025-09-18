package tools.vitruv.methodologisttemplate.consistency;

import brakesystem.BrakeCaliper;
import cad.CADRepository;
import cad.CShape;
import cad.Circle;
import tools.vitruv.methodologisttemplate.consistency.utils.StoexConsistencyHelper;
import tools.vitruv.stoex.stoex.Expression;
import uncertainty.Uncertainty;
import uncertainty.UncertaintyAnnotationRepository;

public class ThroatWidthReactionsHelper {

    public static void updateCShapeThroatWidth(Integer newValue, Integer oldValue, Circle circle,
            UncertaintyAnnotationRepository uncertaintyRepo) {
        // No update needed if oldValue is 0 (initial creation)
        if (oldValue == 0) {
            return;
        }
        CADRepository repo = (CADRepository) circle.eContainer();

        // Find the CShape that is linked to the BrakeCaliper and update its
        // throatWidth
        // There should be exactly one such CShape (Assumption based on the case
        // study)
        CShape cShape = repo.getCadElements().stream().filter(e -> e instanceof CShape).map(e -> (CShape) e)
                .filter(c -> c.getIdentifier().equals("BrakeCaliperCShape")).findFirst().orElseThrow();

        if (handleCircleUncertainty(repo, circle, cShape, oldValue, uncertaintyRepo)) {
            return;
        }
        // No uncertainty involved, just update the throat width directly
        int delta = newValue - oldValue;
        cShape.setThroatWidth(cShape.getThroatWidth() + delta);

    }

    public static boolean handleCircleUncertainty(CADRepository repo, Circle circle, CShape cShape, Integer oldValue,
            UncertaintyAnnotationRepository uncertaintyRepo) {
        Uncertainty circleUncertainty = uncertaintyRepo.getUncertainties().stream()
                .filter(u -> u.getUncertaintyLocation().getReferencedComponents().contains(circle))
                .findFirst().orElse(null);

        if (circleUncertainty != null
                && circleUncertainty.getEffect() != null
                && circleUncertainty.getEffect().getExpression() != null) {
            // create a new uncertainty for the throat width of the CShape
            // This is a simplification, in a real scenario we would need to consider the
            // type of uncertainty and how it propagates
            Uncertainty uncertainty = UncertaintyReactionsHelper.deepCopyUncertainty(circleUncertainty);
            uncertainty.getUncertaintyLocation().getReferencedComponents().add(cShape);
            uncertainty.getUncertaintyLocation().setParameterLocation("throatWidth");

            Expression circleExpression = circleUncertainty.getEffect().getExpression();
            StoexConsistencyHelper stoexHelper = new StoexConsistencyHelper();

            stoexHelper.putVariable("newValue", circleExpression);
            stoexHelper.putVariable("oldValue", oldValue);
            stoexHelper.putVariable("throatWidth", cShape.getThroatWidth());
            Expression newThroatWidthExpression = (Expression) stoexHelper
                    .evaluateToStoexExpression("throatWidth + newValue - oldValue");

            cShape.setThroatWidth(stoexHelper.getMean(newThroatWidthExpression).intValue());

            uncertainty.getEffect().setExpression(newThroatWidthExpression);
            uncertaintyRepo.getUncertainties().add(uncertainty);

            return true;

        }
        return false;
    }

    public static BrakeCaliper createNewBrakeCaliper(CShape cShape) {
        BrakeCaliper brakeCaliper = brakesystem.BrakesystemFactory.eINSTANCE.createBrakeCaliper();
        brakeCaliper.setBridgeGap(cShape.getThroatWidth());
        return brakeCaliper;
    }

}
