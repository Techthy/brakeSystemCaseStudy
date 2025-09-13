package tools.vitruv.methodologisttemplate.consistency.utils;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import brakesystem.BrakeCaliper;
import cad.CADRepository;
import cad.CShape;
import cad.Circle;
import tools.vitruv.stoex.stoex.Expression;
import tools.vitruv.stoex.stoex.NormalDistribution;
import uncertainty.Effect;
import uncertainty.Pattern;
import uncertainty.Uncertainty;
import uncertainty.UncertaintyAnnotationRepository;
import uncertainty.UncertaintyFactory;
import uncertainty.UncertaintyLocation;
import uncertainty.UncertaintyPerspective;

public class ReactionsHelper {

    public static BrakeCaliper createNewBrakeCaliper(CShape cShape) {
        BrakeCaliper brakeCaliper = brakesystem.BrakesystemFactory.eINSTANCE.createBrakeCaliper();
        brakeCaliper.setBridgeGap(cShape.getThroatWidth());
        return brakeCaliper;
    }

    public static void updateCShapeThroatWidth(Integer newValue, Integer oldValue, Circle circle) {
        // No update needed if oldValue is 0 (initial creation)
        if (oldValue == 0) {
            return;
        }
        CADRepository repo = (CADRepository) circle.eContainer();

        UncertaintyAnnotationRepository uncertaintyRepo = null;

        EList<Resource> eObjectList = repo.eResource().getResourceSet().getResources();
        for (Resource resource : eObjectList) {
            EObject eObject = resource.getContents().get(0);
            if (eObject instanceof UncertaintyAnnotationRepository uncertaintyAnnotationRepository) {
                uncertaintyRepo = uncertaintyAnnotationRepository;
            }
        }

        if (uncertaintyRepo == null) {
            throw new IllegalStateException("No UncertaintyAnnotationRepository found in the resource set.");
        }

        Uncertainty circleUncertainty = uncertaintyRepo.getUncertainties().stream()
                .filter(u -> u.getUncertaintyLocation().getReferencedComponents().contains(circle))
                .findFirst().orElse(null);

        // Find the CShape that is linked to the BrakeCaliper and update its
        // throatWidth
        // There should be exactly one such CShape (Assumption based on the case
        // study)
        CShape cShape = repo.getCadElements().stream().filter(e -> e instanceof CShape).map(e -> (CShape) e)
                .filter(c -> c.getIdentifier().equals("BrakeCaliperCShape")).findFirst().orElseThrow();

        if (circleUncertainty != null && circleUncertainty.getEffect() != null
                && circleUncertainty.getEffect().getExpression() != null) {
            // create a new uncertainty for the throat width of the CShape
            // This is a simplification, in a real scenario we would need to consider the
            // type of uncertainty and how it propagates
            Uncertainty uncertainty = copyUncertainty(circleUncertainty);
            uncertainty.getUncertaintyLocation().getReferencedComponents().add(cShape);
            uncertainty.getUncertaintyLocation().setParameterLocation("throatWidth");

            Expression circleExpression = circleUncertainty.getEffect().getExpression();
            StoexConsistencyHelper stoexHelper = new StoexConsistencyHelper();

            stoexHelper.putVariable("newValue", circleExpression);
            stoexHelper.putVariable("oldValue", oldValue);
            stoexHelper.putVariable("throatWidth", cShape.getThroatWidth());
            Expression newThroatWidthExpression = (Expression) stoexHelper
                    .evaluateToStoexExpression("throatWidth + newValue - oldValue");
            // For now assume the result to be a normal distribution
            if (newThroatWidthExpression instanceof NormalDistribution deltaNormal) {
                cShape.setThroatWidth((int) deltaNormal.getMu());
            }

            uncertainty.getEffect().setExpression(newThroatWidthExpression);
            uncertaintyRepo.getUncertainties().add(uncertainty);

        } else {
            int delta = newValue - oldValue;

            cShape.setThroatWidth(cShape.getThroatWidth() + delta);
        }

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
