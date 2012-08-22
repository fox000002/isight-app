package org.huys.isight;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.engineous.sdk.component.ComponentAPI;
import com.engineous.sdk.designdriver.DesignDriverFactory;
import com.engineous.sdk.designdriver.parameters.DesignVariable;
import com.engineous.sdk.designdriver.parameters.Objective;
import com.engineous.sdk.designdriver.plan.OptimizationPlan;
import com.engineous.sdk.log.Log;
import com.engineous.sdk.log.SysLog;
import com.engineous.sdk.metamodel.MetaModel;
import com.engineous.sdk.metamodel.MetaModelManager;
import com.engineous.sdk.model.DtComponent;
import com.engineous.sdk.model.DtControlFlow;
import com.engineous.sdk.model.DtModelManager;
import com.engineous.sdk.model.DtScalarVariable;
import com.engineous.sdk.model.DtUtils;
import com.engineous.sdk.model.DtVariable;
import com.engineous.sdk.server.Logon;
import com.engineous.sdk.vars.EsiTypes;
import com.engineous.sdk.vars.Variable;

/**
 * Example to show programmatically develop and configure a model
 */
public class Modelling {


    //=========================================================================

    /**
     * Main routine runs the tests.
     *
     * @param args
     */
    public static void run(String[] args) {

        try {
            // Must make Library connection
            Logon.initStandalone("ModellingAPIExample");

            // Example of how to load a model that has been saved
//            loadModelFromFile();

            //Example of how to create a model from scratch
            //createModelFromScratch();

            listComponents();

            createMySimOptModelFromScratch();


        } catch (Exception e) {
            SysLog.log(Log.ERROR, e, "Exception caught in main...");
        }

    }

    //=========================================================================

    /**
     * Method listComponents
     *
     * @throws Exception
     */
    //=========================================================================
    private static void listComponents()
        throws Exception {
        Collection<MetaModel> mmc = MetaModelManager.instance().getComponentList();

         for (MetaModel mm : mmc) {
             System.out.println("Component Name : " + mm.getName());
        }


//        Iterator<MetaModel> iter = mmc.iterator();
//
//        while (iter.hasNext()) {
//
//            MetaModel mm = iter.next();
//
//            System.out.println("Component Name : " + mm.getName());
//
//
//            //List<Variable> props = mm.getPropertyList();
//
//            Iterator<Variable> iterVar = props.iterator();
//
//            while (iterVar.hasNext()) {
//                System.out.println("== prop : " + iter.next().getName());
//            }
//        }

    }


    //=========================================================================

    /**
     * Method loadModelFromFile
     *
     * @throws Exception
     */
    //=========================================================================
    private static void loadModelFromFile()
            throws Exception {

        String fileName = "opt-template.zmf";
        // Instantiate model from file
        InputStream modelStream = new FileInputStream(fileName);
        DtModelManager mgr = DtModelManager.createModel(modelStream);
        modelStream.close();

        // Get the root component
        DtComponent rootComponent = mgr.getRootComponent();

        //Make any changes here that you want
        //......

        // If desired save the model here (could use same name here and just overwrite,
        // but this example creates a new file)
        String newName = "opt-template-new.zmf";
        OutputStream savedFile = new FileOutputStream(newName);
        mgr.saveZippedModel(savedFile);

        // Could also launch Gateway directly
        String[] rtArgs = {"rt_gateway", newName};
        Runtime.getRuntime().exec(rtArgs);
    }

    //=========================================================================

    /**
     * Method createMySimOptModelFromScratch
     *
     * @throws Exception
     */
    //=========================================================================
    private static void createMySimOptModelFromScratch()
            throws Exception {
        DtModelManager mgr = DtModelManager.createEmptyModel();
        mgr.getModelProperties().setModelName("My SimOpt Model");

        //
        MetaModel rootMM = MetaModelManager.instance().lookupMetaModel(EsiTypes.OPTIMIZATION_COMPONENT);
        DtComponent myTask = DtModelManager.createComponent(rootMM, "MySimcodeOptimization");
        mgr.setRootComponent(myTask);

        // Now add a calculator to the task
        MetaModel cmdMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.OSCommand");
        DtComponent cmd = DtModelManager.createComponent(cmdMM, "My Cmd");
        myTask.addComponent(cmd);

        //
        MetaModel datexMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Datex");
        DtComponent in = DtModelManager.createComponent(datexMM, "My Input");
        myTask.addComponent(in);
        DtComponent out = DtModelManager.createComponent(datexMM, "My Output");
        myTask.addComponent(out);

        //
        DtControlFlow cf1 = DtModelManager.createControlFlow(null, in);
        DtControlFlow cf2 = DtModelManager.createControlFlow(in, cmd);
        DtControlFlow cf3 = DtModelManager.createControlFlow(cmd, out);
        DtControlFlow cf4 = DtModelManager.createControlFlow(out, null);

        myTask.addControlFlow(cf1);
        myTask.addControlFlow(cf2);
        myTask.addControlFlow(cf3);
        myTask.addControlFlow(cf4);

        //
        DtScalarVariable var = DtModelManager.createScalarVariable("x", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                Variable.MODE_INPUT, null, null);

        var.getValueObj().setValue(100);
        cmd.addParameter(var);

        DtUtils.copyParameters(cmd, myTask);
        DtUtils.copyParameters(cmd, in);
        DtUtils.copyParameters(cmd, out);


        DtScalarVariable var2 = DtModelManager.createScalarVariable("y", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                Variable.MODE_OUTPUT, null, null);

        cmd.addParameter(var2);

        DtScalarVariable varStr = DtModelManager.createScalarVariable("sample_Tmpl", EsiTypes.STRING, Variable.ROLE_PARAMETER,
                Variable.MODE_INPUT, null, null);
        in.addParameter(varStr);

        DtScalarVariable varStr2 = DtModelManager.createScalarVariable("output_dat", EsiTypes.STRING, Variable.ROLE_PARAMETER,
                Variable.MODE_INPUT, null, null);
        out.addParameter(varStr2);

        //
        OptimizationPlan optPlan = DesignDriverFactory.createOptPlan(myTask);
        DesignVariable dv = optPlan.addDesignVariable("x");
        Objective obj = optPlan.addObjective("y", Objective.MINIMIZE);
        optPlan.store(myTask);

        //
        ComponentAPI cmdAPI = cmd.getAPI();
        cmdAPI.set("type", "Command");
        cmdAPI.set("Command", "/path/to/cmd");
        cmdAPI.set("commandargs", "args");
        cmdAPI.apply();

        //
        ComponentAPI inAPI = in.getAPI();
        String pgm1 = "// DATA EXCHANGE PROGRAM - DO NOT EDIT THIS COMMENT\n";
        pgm1 += "// parameter \"x\" as x\n";
        pgm1 += "// parameter \"sample_Tmpl\" as sample_Tmpl\n";
        pgm1 += "//END COMMENT\n";
        pgm1 += "sample2 = new Partitioner(Tool.RANDOM, new FileExchanger(C_, sample_Tmpl, sample_Tmpl), null);\n";
        pgm1 += "sample2.line(6).write(x);\n";
        inAPI.set("program", pgm1);
        inAPI.apply();

        ComponentAPI outAPI = out.getAPI();
        String pgm2 = "// DATA EXCHANGE PROGRAM - DO NOT EDIT THIS COMMENT\n";
        pgm2 += "// parameter \"x\" as x\n";
        pgm2 += "// parameter \"output_dat\" as output_dat\n";
        pgm2 += "//END COMMENT\n";
        pgm2 += "out = new Partitioner(Tool.RANDOM, new FileExchanger(C_, output_dat), null);\n";
        pgm2 += "out.line(6).read(x);\n";
        outAPI.set("program", pgm2);
        outAPI.apply();

        //
        List<DtVariable> varToMap = new ArrayList<DtVariable>();
        varToMap.add(var);
        DtUtils.mapAddedParameterList(varToMap);

        //Save the model
        // To verify what you have created, go load this into the Isight Gateway.

        String newName = "MySimOptModel.zmf";
        OutputStream savedFile = new FileOutputStream(newName);
        mgr.saveZippedModel(savedFile);
    }

    //=========================================================================

    /**
     * Method createMyOptModelFromScratch
     *
     * @throws Exception
     */
    //=========================================================================
    private static void createMyOptModelFromScratch()
        throws Exception {
        DtModelManager mgr = DtModelManager.createEmptyModel();
        mgr.getModelProperties().setModelName("My Optimization Model");

        //
        MetaModel rootMM = MetaModelManager.instance().lookupMetaModel(EsiTypes.OPTIMIZATION_COMPONENT);
        DtComponent myTask = DtModelManager.createComponent(rootMM, "MyOptimization");
        mgr.setRootComponent(myTask);

        // Now add a calculator to the task
        MetaModel calcMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Calculator");
        DtComponent calc = DtModelManager.createComponent(calcMM, "My Calc");
        myTask.addComponent(calc);

        //
        DtControlFlow cf1 = DtModelManager.createControlFlow(null, calc);
        DtControlFlow cf2 = DtModelManager.createControlFlow(calc, null);

        myTask.addControlFlow(cf1);
        myTask.addControlFlow(cf2);

        //
        DtScalarVariable var = DtModelManager.createScalarVariable("x", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                Variable.MODE_INPUT, null, null);

        var.getValueObj().setValue(100);
        calc.addParameter(var);

        DtUtils.copyParameters(calc, myTask);

        DtScalarVariable var2 = DtModelManager.createScalarVariable("y", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                Variable.MODE_OUTPUT, null, null);

        calc.addParameter(var2);

        //
        OptimizationPlan optPlan = DesignDriverFactory.createOptPlan(myTask);
        DesignVariable dv = optPlan.addDesignVariable("x");
        Objective obj = optPlan.addObjective("y", Objective.MINIMIZE);
        optPlan.store(myTask);

        //
        ComponentAPI calculatorAPI = calc.getAPI();
        String calc1 = "y=x^2";
        calculatorAPI.set("expression", calc1);
        calculatorAPI.apply();

        //
        List<DtVariable> varToMap = new ArrayList<DtVariable>();
        varToMap.add(var);
        DtUtils.mapAddedParameterList(varToMap);

        //Save the model
        // To verify what you have created, go load this into the Isight Gateway.

        String newName = "MyOptModel.zmf";
        OutputStream savedFile = new FileOutputStream(newName);
        mgr.saveZippedModel(savedFile);
    }


    //=========================================================================

    /**
     * Method createModelFromScratch
     *
     * @throws Exception
     */
    //=========================================================================
    private static void createModelFromScratch()
            throws Exception {

        DtModelManager mgr = DtModelManager.createEmptyModel();
        mgr.getModelProperties().setModelName("My Model");

        //Create the root component (Just a Task here, but could be anything)
        //First find the MetaModel for the Task
        MetaModel taskMM = MetaModelManager.instance().lookupMetaModel(EsiTypes.TASK_COMPONENT);

        DtComponent myTask = DtModelManager.createComponent(taskMM, "My root task");
        mgr.setRootComponent(myTask);

        //Now lets add a Simcode to the Task
        MetaModel simCodeMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Simcode");
        DtComponent sim1 = DtModelManager.createComponent(simCodeMM, "Simcode1");

        //Note: This only adds it as a child of the Task - it does not put it in a workflow
        myTask.addComponent(sim1);

        //Now lets add a Calculator to the Task
        MetaModel calcMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Calculator");
        DtComponent calc = DtModelManager.createComponent(calcMM, "My Calc");
        myTask.addComponent(calc);

        //Now lets add a DOE with an Excel inside of it
        MetaModel doeMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.DOE");
        DtComponent doe = DtModelManager.createComponent(doeMM, "DOE on Excel");
        MetaModel excelMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Excel");
        DtComponent excel = DtModelManager.createComponent(excelMM, "Excel 1");

        doe.addComponent(excel);
        myTask.addComponent(doe);

        //Now the hierarchy is established, but we must build the actual control flows
        //Note: use "null" for the begin and end nodes
        DtControlFlow cf1 = DtModelManager.createControlFlow(null, excel);
        DtControlFlow cf2 = DtModelManager.createControlFlow(excel, null);
        doe.addControlFlow(cf1);
        doe.addControlFlow(cf2);

        cf1 = DtModelManager.createControlFlow(null, sim1);
        cf2 = DtModelManager.createControlFlow(null, calc);
        DtControlFlow cf3 = DtModelManager.createControlFlow(calc, null);
        DtControlFlow cf4 = DtModelManager.createControlFlow(sim1, doe);
        DtControlFlow cf5 = DtModelManager.createControlFlow(doe, null);
        myTask.addControlFlow(cf1);
        myTask.addControlFlow(cf2);
        myTask.addControlFlow(cf3);
        myTask.addControlFlow(cf4);
        myTask.addControlFlow(cf5);

        //Add some parameters to the Task
        DtScalarVariable var = DtModelManager.createScalarVariable("a", EsiTypes.REAL, Variable.ROLE_PARAMETER, Variable.MODE_INPUT, null, null);
        var.getValueObj().setValue(3);
        myTask.addParameter(var);

        DtUtils.copyParameters(myTask, calc);
        DtUtils.copyParameters(myTask, sim1);

        //The above calls just created those variables in the children
        //Need to create the mappings.  There are APIs to create DataFlow objects and add
        //them to the components, but we provide a convenient DtUtils method to do this
        List<DtVariable> varsToMap = new ArrayList<DtVariable>();
        varsToMap.add(var);
        DtUtils.mapAddedParameterList(varsToMap);

        //Apply some conditional workflow
        cf1 = myTask.getControlFlow(null, "My Calc");
        cf2 = myTask.getControlFlow(null, "Simcode1");
        //The special syntax is to break up the various parts of the expression for easy parsing later
        //The "/C" indicates constant on the right side
        //Use "/V" if comparing against another Variable (parameter)
        cf1.setCondition("<parent>.a/>/C/1");
        cf1.setLabel("a > 1");
        cf2.setCondition("<parent>.a/<=/C/1");
        cf2.setLabel("a <= 1");


        //Save the model
        // To verify what you have created, go load this into the Isight Gateway.

        String newName = "MyNewModel.zmf";
        OutputStream savedFile = new FileOutputStream(newName);
        mgr.saveZippedModel(savedFile);
    }

}

