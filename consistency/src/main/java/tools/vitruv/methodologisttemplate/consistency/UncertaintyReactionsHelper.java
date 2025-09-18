package tools.vitruv.methodologisttemplate.consistency;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import uncertainty.Effect;
import uncertainty.Pattern;
import uncertainty.Uncertainty;
import uncertainty.UncertaintyAnnotationRepository;
import uncertainty.UncertaintyFactory;
import uncertainty.UncertaintyLocation;
import uncertainty.UncertaintyPerspective;

public class UncertaintyReactionsHelper {

    public static List<Uncertainty> createUncertaintyForReferencedComponent(UncertaintyAnnotationRepository repo,
            Iterable<EObject> correspondingReferenceList,
            Uncertainty sourceUncertainty) {

        List<Uncertainty> createdUncertainties = new ArrayList<>();

        List<UncertaintyLocation> existingUncertaintyLocations = repo.getUncertainties().stream()
                .map(u -> u.getUncertaintyLocation())
                .toList();

        for (EObject correspondingElement : correspondingReferenceList) {
            List<UncertaintyLocation> matchingLocations = existingUncertaintyLocations.stream()
                    .filter(loc -> loc.getReferencedComponents().contains(correspondingElement))
                    .toList();

            if (matchingLocations.isEmpty()) {
                // No matching UncertaintyLocation found -> create new Uncertainty and
                // UncertaintyLocation
                Uncertainty newUncertainty = deepCopyUncertainty(sourceUncertainty);
                newUncertainty.getUncertaintyLocation().getReferencedComponents().add(correspondingElement);
                repo.getUncertainties().add(newUncertainty);
                createdUncertainties.add(newUncertainty);
            } else {
                // Check if there is any existing Uncertainty that matches sourceUncertainty
                boolean existingMatch = false;
                for (UncertaintyLocation loc : matchingLocations) {
                    existingMatch = compareUncertainties(sourceUncertainty, (Uncertainty) loc.eContainer());
                    if (existingMatch) {
                        break;
                    }
                }
                if (!existingMatch) {
                    // Different Uncertainty found, create a new one and new UncertaintyLocation
                    Uncertainty newUncertainty = deepCopyUncertainty(sourceUncertainty);
                    newUncertainty.getUncertaintyLocation().getReferencedComponents().add(correspondingElement);
                    repo.getUncertainties().add(newUncertainty);
                    createdUncertainties.add(newUncertainty);
                }
            }
        }

        return createdUncertainties;
    }

    /**
     * Compares two Uncertainty objects for equality based on their attributes and
     * nested elements.
     *
     * @param u1 The first Uncertainty to compare.
     * @param u2 The second Uncertainty to compare.
     * @return true if the Uncertainties are considered equal, false otherwise.
     */
    private static boolean compareUncertainties(Uncertainty u1, Uncertainty u2) {
        if (u1.getKind() != u2.getKind()) {
            return false;
        }
        if (u1.getNature() != u2.getNature()) {
            return false;
        }
        if (u1.getReducability() != u2.getReducability()) {
            return false;
        }
        if (u1.getOnDelete() != u2.getOnDelete()) {
            return false;
        }
        Effect e1 = u1.getEffect();
        Effect e2 = u2.getEffect();
        if (e1 == null || e2 == null) {
            if (e1 != e2) { // one is null, the other is not
                return false;
            }
        } else {
            if (e1.getRepresentation() != e2.getRepresentation()) {
                return false;
            }
            if (e1.getStochasticity() != e2.getStochasticity()) {
                return false;
            }
        }
        Pattern p1 = u1.getPattern();
        Pattern p2 = u2.getPattern();
        if (p1 == null || p2 == null) {
            if (p1 != p2) { // one is null, the other is not
                return false;
            }
        } else {
            if (p1.getPatternType() != p2.getPatternType()) {
                return false;
            }
        }
        UncertaintyPerspective up1 = u1.getPerspective();
        UncertaintyPerspective up2 = u2.getPerspective();
        if (up1 == null || up2 == null) {
            if (up1 != up2) { // one is null, the other is not
                return false;
            }
        } else {
            if (up1.getPerspective() != up2.getPerspective()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Deep copies an Uncertainty including its nested elements.
     * Note: The referenced components in UncertaintyLocation are not copied as
     * it is assumed that the new Uncertainty will reference different components.
     *
     * @param original The original Uncertainty to copy.
     * @return A deep copy of the original Uncertainty.
     */
    public static Uncertainty deepCopyUncertainty(Uncertainty original) {
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
