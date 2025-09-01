package tools.vitruv.methodologisttemplate.consistency.utils;

import java.util.HashMap;
import java.util.Map;

import brakesystem.BrakeDisk;
import cad.CadFactory;
import cad.Circle;
import tools.vitruv.stoex.interpreter.StoexEvaluator;
import tools.vitruv.stoex.interpreter.operations.AddOperation;
import tools.vitruv.stoex.stoex.Expression;
import uncertainty.Uncertainty;

/**
 * Utility class for using Stoex expressions in consistency transformations.
 * This class provides helper methods to evaluate stoex expressions in the
 * context
 * of brake system uncertainty analysis and CAD model synchronization.
 */
public class StoexConsistencyHelper {

    private final StoexEvaluator stoexEvaluator;
    private final Map<String, Object> variables;

    public StoexConsistencyHelper() {
        this.stoexEvaluator = new StoexEvaluator();
        this.variables = new HashMap<>();
    }

    public void putVariable(String name, Object value) {
        variables.put(name, value);
    }

    public Object computeAdditionExpression(Expression left, Expression right) {
        AddOperation op = new AddOperation();
        return op.evaluate(left, right);
    }

    /**
     * Evaluates a stoex expression with the given variables.
     * 
     * @param expression The stoex expression as a string
     * @return The evaluation result
     */
    public String evaluateExpression(String expression) {
        Object result = stoexEvaluator.evaluate(expression, variables);
        return stoexEvaluator.serialize(result);
    }


    /**
     * Calculates brake disk circumference with uncertainty propagation.
     * 
     * @param brakeDisk            The brake disk model element
     * @param uncertaintyTolerance The uncertainty tolerance in mm
     * @return Map containing nominal value and uncertainty bounds
     */
    public Map<String, Double> calculateBrakeDiskCircumferenceWithUncertainty(
            BrakeDisk brakeDisk, double uncertaintyTolerance) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("diameter", (double) brakeDisk.getDiameterInMM());
        variables.put("pi", Math.PI);
        variables.put("tolerance", uncertaintyTolerance);

        // Calculate nominal circumference
        Object nominalResult = stoexEvaluator.evaluate("diameter * pi", variables);
        double nominalCircumference = ((Number) nominalResult).doubleValue();

        // Calculate uncertainty bounds
        Object minResult = stoexEvaluator.evaluate("(diameter - tolerance) * pi", variables);
        Object maxResult = stoexEvaluator.evaluate("(diameter + tolerance) * pi", variables);

        Map<String, Double> result = new HashMap<>();
        result.put("nominal", nominalCircumference);
        result.put("min", ((Number) minResult).doubleValue());
        result.put("max", ((Number) maxResult).doubleValue());

        return result;
    }

    /**
     * Transforms brake disk dimensions to CAD circle with uncertainty
     * consideration.
     * 
     * @param brakeDisk   Source brake disk
     * @param uncertainty Associated uncertainty model
     * @return CAD Circle with calculated radius
     */
    public Circle transformBrakeDiskToCircleWithUncertainty(BrakeDisk brakeDisk, Uncertainty uncertainty) {
        Circle circle = CadFactory.eINSTANCE.createCircle();

        // Base calculation
        Map<String, Object> variables = new HashMap<>();
        variables.put("diameter", (double) brakeDisk.getDiameterInMM());

        // Calculate radius
        Object radiusResult = stoexEvaluator.evaluate("diameter / 2", variables);
        int radius = ((Number) radiusResult).intValue();

        // Set circle properties
        circle.setRadius(radius);

        // Set identifier and description based on uncertainty
        if (uncertainty != null) {
            circle.setIdentifier("BrakeDisk_R" + radius + "_WithUncertainty");
            circle.setDescription("Brake disk converted to circle with uncertainty considerations");
        } else {
            circle.setIdentifier("BrakeDisk_R" + radius);
            circle.setDescription("Brake disk converted to circle");
        }

        return circle;
    }

    /**
     * Validates brake system performance constraints using stoex expressions.
     * 
     * @param brakeDisk   The brake disk to validate
     * @param maxDiameter Maximum allowed diameter
     * @param minDiameter Minimum allowed diameter
     * @return true if constraints are satisfied
     */
    public boolean validateBrakeDiskConstraints(BrakeDisk brakeDisk, double maxDiameter, double minDiameter) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("diameter", (double) brakeDisk.getDiameterInMM());
        variables.put("maxDiam", maxDiameter);
        variables.put("minDiam", minDiameter);

        // Use stoex to evaluate constraint expressions
        Object maxCheckResult = stoexEvaluator.evaluate("diameter <= maxDiam", variables);
        Object minCheckResult = stoexEvaluator.evaluate("diameter >= minDiam", variables);

        boolean maxConstraintSatisfied = (Boolean) maxCheckResult;
        boolean minConstraintSatisfied = (Boolean) minCheckResult;

        return maxConstraintSatisfied && minConstraintSatisfied;
    }

    /**
     * Calculates derived properties for CAD synchronization.
     * 
     * @param expression       The stoex expression to evaluate
     * @param brakeDisk        Source brake disk providing base values
     * @param additionalParams Additional parameters for the calculation
     * @return The calculated result
     */
    public Object calculateDerivedProperty(String expression, BrakeDisk brakeDisk,
            Map<String, Object> additionalParams) {
        Map<String, Object> variables = new HashMap<>();

        // Add brake disk properties
        variables.put("diameter", (double) brakeDisk.getDiameterInMM());
        variables.put("thickness", (double) brakeDisk.getBrakeDiskThicknessInMM());
        variables.put("centeringDiameter", (double) brakeDisk.getCenteringDiameterInMM());
        variables.put("boltHoleCircle", (double) brakeDisk.getBoltHoleCircleInMM());
        variables.put("rimHoles", (double) brakeDisk.getRimHoleNumber());

        // Add additional parameters
        if (additionalParams != null) {
            variables.putAll(additionalParams);
        }

        return stoexEvaluator.evaluate(expression, variables);
    }
}
