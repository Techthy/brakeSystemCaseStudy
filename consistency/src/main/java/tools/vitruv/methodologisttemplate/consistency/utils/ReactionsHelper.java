package tools.vitruv.methodologisttemplate.consistency.utils;

import brakesystem.BrakeCaliper;
import cad.CADRepository;
import cad.CShape;

public class ReactionsHelper {

    public static BrakeCaliper createNewBrakeCaliper(CShape cShape) {
        BrakeCaliper brakeCaliper = brakesystem.BrakesystemFactory.eINSTANCE.createBrakeCaliper();
        brakeCaliper.setBridgeGap(cShape.getThroatWidth());
        return brakeCaliper;
    }

    public static void updateCShapeThroatWidth(Integer newValue, Integer oldValue, CADRepository repo) {
        // No update needed if oldValue is 0 (initial creation)
        if (oldValue == 0) {
            return;
        }
        int delta = newValue - oldValue;
        // Find the CShape that is linked to the BrakeCaliper and update its throatWidth
        // There should be exactly one such CShape (Assumption based on the case study)
        CShape cShape = repo.getCadElements().stream().filter(e -> e instanceof CShape).map(e -> (CShape) e)
                .filter(c -> c.getIdentifier().equals("BrakeCaliperCShape")).findFirst().orElseThrow();
        cShape.setThroatWidth(cShape.getThroatWidth() + delta);
    }
}
