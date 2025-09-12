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
        int delta = newValue - oldValue;
        for (CShape cShape : repo.getCadElements().stream().filter(e -> e instanceof CShape).map(e -> (CShape) e)
                .toList()) {
            cShape.setThroatWidth(cShape.getThroatWidth() + delta);
        }
    }
}
