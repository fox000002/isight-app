package org.huys.isight;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.engineous.sdk.designdriver.DesignDriverFactory;
import com.engineous.sdk.designdriver.exceptions.DesignDriverException;
import com.engineous.sdk.designdriver.parameters.DesignVariable;
import com.engineous.sdk.designdriver.parameters.Factor;
import com.engineous.sdk.designdriver.parameters.Objective;
import com.engineous.sdk.designdriver.parameters.OutputConstraint;
import com.engineous.sdk.designdriver.parameters.Response;
import com.engineous.sdk.designdriver.plan.DOEPlan;
import com.engineous.sdk.designdriver.plan.DesignEvalResults;
import com.engineous.sdk.designdriver.plan.DesignEvaluation;
import com.engineous.sdk.designdriver.plan.OptimizationPlan;
import com.engineous.sdk.designdriver.plan.PlanResults;
import com.engineous.sdk.designdriver.techniques.OptimizationTechnique;
import com.engineous.sdk.log.Log;
import com.engineous.sdk.log.SysLog;
import com.engineous.sdk.model.DtComponent;
import com.engineous.sdk.model.DtModelManager;
import com.engineous.sdk.runtime.Context;
import com.engineous.sdk.server.Logon;
import com.engineous.sdk.vars.Variable;
import com.engineous.sdk.vars.VariableException;


/**
 * Example to show programmitically configuring an optimization and DOE component
 * within an existing template model
 */
public class DesignDriverAPI {


    //=========================================================================

    /**
     * Main routine runs the tests.
     *
     * @param args
     */
    public static void main(String[] args) {

        // These could be sent in through the args or read from a file right here
        String fileName = "opt-template.zmf";
        String dvName = "x1";
        double lower = 5.0;
        String maxIter = "20";

        try {
            // Must make Library connection
            Logon.initStandalone();

            testOptPlan();
            testDOEPlan();

            // Instantiate model from file
            InputStream modelStream = new FileInputStream(fileName);
            DtModelManager mgr = DtModelManager.createModel(modelStream);
            modelStream.close();

            // Get the optimization component (assumed to be the root here; if it is not
            // the root you can dig through the hierarchy using the getComponent(name) method
            // to get the child component by name
            DtComponent optComponent = mgr.getRootComponent();
            OptimizationPlan optPlan = DesignDriverFactory.createOptPlan(optComponent);

            // Change the design variable lower bound
            DesignVariable dv = (DesignVariable) optPlan.getDesignParameter(OptimizationPlan.DESIGN_VARIABLE, dvName);
            dv.setBound(DesignVariable.LOWER_BOUND, lower);
            dv.setBound(DesignVariable.UPPER_BOUND, 10.0);

            //Change the scale factor on the objective to 2.
            Objective obj = (Objective) optPlan.getDesignParameter(OptimizationPlan.OBJECTIVE, "y");
            obj.setAttributeValue(Objective.SCALE_FACTOR, 2.0);
            obj.setAttributeValue(Objective.WEIGHT_FACTOR, .5);

            // Could even change technique if you want.
            optPlan.setTechnique("com.engineous.plugin.optimization.HookeJeeves");

            // Change the technique option
            OptimizationTechnique tech = (OptimizationTechnique) optPlan.getTechnique();
            tech.getOption("Max Iterations").setValue(maxIter);

            // Must store the new plan configuration back into the component
            optPlan.store(optComponent);

            // If desired save the model here (could use same name here and just overwrite,
            // but this example creates a new file)
            String newName = "opt-template-new.zmf";
            OutputStream savedFile = new FileOutputStream(newName);
            mgr.saveZippedModel(savedFile);

            // Could also launch Gateway directly
            String[] rtArgs = {"rt_gateway", newName};
            Runtime.getRuntime().exec(rtArgs);
        } catch (Exception e) {
            SysLog.log(Log.ERROR, e, "Exception caught in main...");
        }

    }

    //=========================================================================

    /**
     * This example shows how to create a new Optimization Plan, configure it, and execute it
     * on some specified "analysis" that is not an Isight sim-flow of components
     */
    //=========================================================================
    private static void testOptPlan() {

        try {
            OptimizationPlan optPlan = DesignDriverFactory.createOptPlan();
            optPlan.setTechnique("com.engineous.plugin.optimization.HookeJeeves");

            DesignVariable dv = optPlan.addDesignVariable("x1");
            dv.setBound(DesignVariable.LOWER_BOUND, 2.0);
            dv.setBound(DesignVariable.UPPER_BOUND, 10.0);

            @SuppressWarnings("unused") OutputConstraint con = optPlan.addConstraint("y1", OutputConstraint.ATTRIBUTE_LOWER_BOUND, 8);

            Objective obj = optPlan.addObjective("y1", Objective.MINIMIZE);

            ArrayList<Variable> parmList = new ArrayList<Variable>();
            parmList.add(dv.getVariableReference().getVariable());
            parmList.add(obj.getVariableReference().getVariable());

            DesignEvaluation eval = new DesignEvaluation() {

                protected int evaluateDesign(Context context)
                        throws DesignDriverException {

                    try {
                        double val = context.getScalar("x1").getValueObj().getAsReal();
                        context.getScalar("y1").getValueObj().setValue(Math.pow(val - 7, 2));
                        return DesignEvalResults.WORK_CC_OK;
                    } catch (VariableException ve) {
                        throw new DesignDriverException(ve);
                    }
                }
            };
            PlanResults results = optPlan.executePlan(eval, parmList);
            String summary = results.getSummary();
            System.out.println(summary);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //=========================================================================

    /**
     * This example shows how to create a new DOE Plan, configure it, and execute it
     * on some specified "analysis" that is not an Isight sim-flow of components
     */
    //=========================================================================
    private static void testDOEPlan() {

        try {
            DOEPlan doePlan = DesignDriverFactory.createDOEPlan();
            doePlan.setTechnique("com.engineous.plugin.doe.FullFactorial");

            Factor f1 = doePlan.addFactor("x1");

            // Note: You must send in true to update attributes in order to sync all attributes to this change
            f1.setAttributeValue(Factor.ATTRIBUTE_NUM_LEVELS, 7, true);
            Response r1 = doePlan.addResponse("y1");

            ArrayList<Variable> parmList = new ArrayList<Variable>();
            parmList.add(f1.getVariableReference().getVariable());
            parmList.add(r1.getVariableReference().getVariable());


            DesignEvaluation eval = new DesignEvaluation() {

                protected int evaluateDesign(Context context)
                        throws DesignDriverException {

                    try {
                        double val = context.getScalar("x1").getValueObj().getAsReal();
                        context.getScalar("y1").getValueObj().setValue(val * 10);
                        return DesignEvalResults.WORK_CC_OK;
                    } catch (VariableException ve) {
                        throw new DesignDriverException(ve);
                    }

                }
            };
            PlanResults results = doePlan.executePlan(eval, parmList);
            String summary = results.getSummary();
            System.out.println(summary);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

