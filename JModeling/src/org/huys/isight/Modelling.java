package org.huys.isight;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.engineous.sdk.component.ComponentAPI;
import com.engineous.sdk.component.PluginAPI;
import com.engineous.sdk.designdriver.DesignDriverFactory;
import com.engineous.sdk.designdriver.parameters.DesignVariable;
import com.engineous.sdk.designdriver.parameters.Objective;
import com.engineous.sdk.designdriver.parameters.OutputConstraint;
import com.engineous.sdk.designdriver.plan.OptimizationPlan;
import com.engineous.sdk.log.Log;
import com.engineous.sdk.log.SysLog;
import com.engineous.sdk.metamodel.MetaModel;
import com.engineous.sdk.metamodel.MetaModelManager;
import com.engineous.sdk.model.*;
import com.engineous.sdk.server.Logon;
import com.engineous.sdk.vars.*;

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
            System.out.println("calling createModelFromScratch");
            createModelFromScratch();

            listComponents();

            createMySimCodeOptModelFromScratch();

            createMyWingDOptModelFromScratch();

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

    private static DtScalarVariable AddInputParameterReal(String varName)
            throws Exception {
        return DtModelManager.createScalarVariable(varName, EsiTypes.REAL, Variable.ROLE_PARAMETER,
                Variable.MODE_INPUT, null, null);
    }

    private static DtScalarVariable AddOutputParameterReal(String varName)
            throws Exception {
        return DtModelManager.createScalarVariable(varName, EsiTypes.REAL, Variable.ROLE_PARAMETER,
                Variable.MODE_OUTPUT, null, null);
    }

    private static DtScalarVariable AddInputOutputParameterReal(String varName)
            throws Exception {
        return DtModelManager.createScalarVariable(varName, EsiTypes.REAL, Variable.ROLE_PARAMETER,
                Variable.MODE_INOUT, null, null);
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

        MetaModel cmdMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.OSCommand");
        DtComponent cmd = DtModelManager.createComponent(cmdMM, "My Cmd");
        myTask.addComponent(cmd);

        //
        MetaModel datexMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Datex");
        DtComponent in = DtModelManager.createComponent(datexMM, "My Input");
        myTask.addComponent(in);
        DtComponent out = DtModelManager.createComponent(datexMM, "My Output");
        myTask.addComponent(out);

        // Now add a calculator to the task
        MetaModel calcMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Calculator");
        DtComponent calc = DtModelManager.createComponent(calcMM, "My Calc");
        myTask.addComponent(calc);

        //
        DtControlFlow cf1 = DtModelManager.createControlFlow(null, in);
        DtControlFlow cf2 = DtModelManager.createControlFlow(in, cmd);
        DtControlFlow cf3 = DtModelManager.createControlFlow(cmd, out);
        DtControlFlow cf4 = DtModelManager.createControlFlow(out, calc);
        DtControlFlow cf5 = DtModelManager.createControlFlow(calc, null);

        myTask.addControlFlow(cf1);
        myTask.addControlFlow(cf2);
        myTask.addControlFlow(cf3);
        myTask.addControlFlow(cf4);
        myTask.addControlFlow(cf5);

        //
        DtScalarVariable var = DtModelManager.createScalarVariable("k", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                Variable.MODE_OUTPUT, null, null);

        //var.getValueObj().setValue(100);
        calc.addParameter(var);

        DtUtils.copyParameters(calc, myTask);
        //DtUtils.copyParameters(calc, in);
        //DtUtils.copyParameters(calc, out);


        //DtScalarVariable var2 = DtModelManager.createScalarVariable("y", EsiTypes.REAL, Variable.ROLE_PARAMETER,
       //         Variable.MODE_OUTPUT, null, null);
        //calc.addParameter(var2);



     //   DtScalarVariable varStr = DtModelManager.createScalarVariable("deform_d", EsiTypes.FILE, Variable.ROLE_PARAMETER,
    //            Variable.MODE_OUTPUT, null, null);
    //    varStr.getValueObj().setValue("{modeldir}\\deform.d");
    //    in.addParameter(varStr);
  /*
        DtScalarVariable varStr2 = DtModelManager.createScalarVariable("afcharct_dat", EsiTypes.FILE, Variable.ROLE_PARAMETER,
                Variable.MODE_INPUT, null, null);
        varStr2.getValueObj().setValue("{modeldir}\\afcharct.dat");
        out.addParameter(varStr2);

        DtScalarVariable varStr3 = DtModelManager.createScalarVariable("beamthick", EsiTypes.FILE, Variable.ROLE_PARAMETER,
                Variable.MODE_INPUT, null, null);
        varStr3.getValueObj().setValue("{modeldir}\\beamthick");
        out.addParameter(varStr3);

        DtScalarVariable varStr4 = DtModelManager.createScalarVariable("Cl_polar_dat", EsiTypes.FILE, Variable.ROLE_PARAMETER,
                Variable.MODE_INOUT, null, null);
        varStr4.getValueObj().setValue("{modeldir}\\Cl_polar.dat");
        out.addParameter(varStr4);
*/
        //
        ComponentAPI cmdAPI = cmd.getAPI();
        cmdAPI.set("type", "Command");
        cmdAPI.set("Command", "E:\\Working\\isight-app\\JModeling\\run_Xfoil.bat");
        //cmdAPI.set("workingdir", "E:\\Working\\isight-app\\JModeling");
        //DtScalarVariable dtvar = (DtScalarVariable)cmd.getProperty("workingdir");
        //Value v = dtvar.getValueObj();
        //v.setValue("E:\\Working\\isight-app\\JModeling");
        //ModelProperties mp = ModelProperties.getModelProperties(cmd);
      //  System.out.println(mp.getModelRunDir());
       // mp.setModelRunDir("E:\\Working\\isight-app\\JModeling");
        //System.out.println(mp.getModelRunDir());
       // DtScalarVariable dtvar = (DtScalarVariable)cmd.getProperty( DtComponent.PROPERTY_WORKING_DIR);
       // dtvar.getValueObj().setValue("E:\\Working\\isight-app\\JModeling");

        //cmd.PROPERTY_WORKING_DIR = "E:\\Working\\isight-app\\JModeling";

        cmdAPI.apply();

        //
        DtScalarVariable v_down01 = AddInputParameterReal("v_down01");
        in.addParameter(v_down01);

        DtScalarVariable v_down02 = AddInputParameterReal("v_down02");
        in.addParameter(v_down02);

        DtScalarVariable v_down03 = AddInputParameterReal("v_down03");
        in.addParameter(v_down03);

        DtScalarVariable v_down04 = AddInputParameterReal("v_down04");
        in.addParameter(v_down04);

        DtScalarVariable v_down05 = AddInputParameterReal("v_down05");
        in.addParameter(v_down05);

        DtScalarVariable v_up01 = AddInputParameterReal("v_up01");
        in.addParameter(v_up01);

        DtScalarVariable v_up02 = AddInputParameterReal("v_up02");
        in.addParameter(v_up02);

        DtScalarVariable v_up03 = AddInputParameterReal("v_up03");
        in.addParameter(v_up03);

        DtScalarVariable v_up04 = AddInputParameterReal("v_up04");
        in.addParameter(v_up04);

        DtScalarVariable v_up05 = AddInputParameterReal("v_up05");
        in.addParameter(v_up05);

        ComponentAPI inAPI = in.getAPI();
        String pgm1 = "// DATA EXCHANGE PROGRAM - DO NOT EDIT THIS COMMENT\n" +
                "//parameter \"v_down01\" as v_down01\n" +
                "//parameter \"v_down02\" as v_down02\n" +
                "//parameter \"v_down03\" as v_down03\n" +
                "//parameter \"v_down04\" as v_down04\n" +
                "//parameter \"v_down05\" as v_down05\n" +
                "//parameter \"v_up01\" as v_up01\n" +
                "//parameter \"v_up02\" as v_up02\n" +
                "//parameter \"v_up03\" as v_up03\n" +
                "//parameter \"v_up04\" as v_up04\n" +
                "//parameter \"v_up05\" as v_up05\n" +
                "//END COMMENT\n" +
                "deform = new Partitioner(Tool.RANDOM, new FileExchanger(C_, Exchanger.PUT, \"deform.d_tmpl\", \"deform.d\"), null);\n" +
                "deform.word(new LineLocator(1, new StringLocator(\"(涓嬬考闈¢)\", Locator.SOP)), 2).write(v_down01);\n" +
                "deform.word(new LineLocator(3, new StringLocator(\"(涓嬬考闈¢)\", Locator.SOP)), 2).write(v_down02);\n" +
                "deform.word(new LineLocator(5, new StringLocator(\"(涓嬬考闈¢)\", Locator.SOP)), 2).write(v_down03);\n" +
                "deform.word(new LineLocator(7, new StringLocator(\"(涓嬬考闈¢)\", Locator.SOP)), 2).write(v_down04);\n" +
                "deform.word(new LineLocator(9, new StringLocator(\"(涓嬬考闈¢)\", Locator.SOP)), 2).write(v_down05);\n" +
                "deform.word(new LineLocator(1, new StringLocator(\"(涓婄考闈¢)\", Locator.SOP)), 2).write(v_up01);\n" +
                "deform.word(new LineLocator(3, new StringLocator(\"(涓婄考闈¢)\", Locator.SOP)), 2).write(v_up02);\n" +
                "deform.word(new LineLocator(5, new StringLocator(\"(涓婄考闈¢)\", Locator.SOP)), 2).write(v_up03);\n" +
                "deform.word(new LineLocator(7, new StringLocator(\"(涓婄考闈¢)\", Locator.SOP)), 2).write(v_up04);\n" +
                "deform.word(new LineLocator(9, new StringLocator(\"(涓婄考闈¢)\", Locator.SOP)), 2).write(v_up05);\n";
        inAPI.set("program", pgm1);
        inAPI.apply();

        DtScalarVariable Cd = AddOutputParameterReal("Cd");
        out.addParameter(Cd);

        DtScalarVariable Cl = AddOutputParameterReal("Cl");
        out.addParameter(Cl);

        DtUtils.copyParameters(out, calc);
        ComponentAPI calculatorAPI = calc.getAPI();
        String calcExp = "k=Cl/Cd";
        calculatorAPI.set("expression", calcExp);
        calculatorAPI.apply();

        DtScalarVariable maxthick = AddOutputParameterReal("maxthick");
        out.addParameter(maxthick);

        DtScalarVariable beamthick = AddOutputParameterReal("beamthick");
        out.addParameter(beamthick);

        DtScalarVariable xc = AddOutputParameterReal("xc");
        out.addParameter(xc);

        //
        DtUtils.copyParameters(in, myTask);
        DtUtils.copyParameters(out, myTask);
        DtUtils.copyParameters(calc, myTask);
        OptimizationPlan optPlan = DesignDriverFactory.createOptPlan(myTask);
        optPlan.setTechnique("com.engineous.plugin.optimization.Miga");
        DesignVariable dv1 = optPlan.addDesignVariable("v_down01");
        dv1.setBound(DesignVariable.LOWER_BOUND, -0.05);
        dv1.setBound(DesignVariable.UPPER_BOUND, 0.05);
        DesignVariable dv2 = optPlan.addDesignVariable("v_down02");
        dv2.setBound(DesignVariable.LOWER_BOUND, -0.05);
        dv2.setBound(DesignVariable.UPPER_BOUND, 0.05);
        DesignVariable dv3 = optPlan.addDesignVariable("v_down03");
        dv3.setBound(DesignVariable.LOWER_BOUND, -0.05);
        dv3.setBound(DesignVariable.UPPER_BOUND, 0.05);
        DesignVariable dv4 = optPlan.addDesignVariable("v_down04");
        dv4.setBound(DesignVariable.LOWER_BOUND, -0.05);
        dv4.setBound(DesignVariable.UPPER_BOUND, 0.05);
        DesignVariable dv5 = optPlan.addDesignVariable("v_down05");
        dv5.setBound(DesignVariable.LOWER_BOUND, -0.05);
        dv5.setBound(DesignVariable.UPPER_BOUND, 0.05);

        DesignVariable dv6 = optPlan.addDesignVariable("v_up01");
        dv6.setBound(DesignVariable.LOWER_BOUND, -0.05);
        dv6.setBound(DesignVariable.UPPER_BOUND, 0.05);
        DesignVariable dv7 = optPlan.addDesignVariable("v_up02");
        dv7.setBound(DesignVariable.LOWER_BOUND, -0.05);
        dv7.setBound(DesignVariable.UPPER_BOUND, 0.05);
        DesignVariable dv8 = optPlan.addDesignVariable("v_up03");
        dv8.setBound(DesignVariable.LOWER_BOUND, -0.05);
        dv8.setBound(DesignVariable.UPPER_BOUND, 0.05);
        DesignVariable dv9 = optPlan.addDesignVariable("v_up04");
        dv9.setBound(DesignVariable.LOWER_BOUND, -0.05);
        dv9.setBound(DesignVariable.UPPER_BOUND, 0.05);
        DesignVariable dv10 = optPlan.addDesignVariable("v_up05");
        dv10.setBound(DesignVariable.LOWER_BOUND, -0.05);
        dv10.setBound(DesignVariable.UPPER_BOUND, 0.05);

        OutputConstraint con1 = optPlan.addConstraint("beamthick", OutputConstraint.ATTRIBUTE_LOWER_BOUND, 0.1);
        OutputConstraint con2 = optPlan.addConstraint("beamthick", OutputConstraint.ATTRIBUTE_UPPER_BOUND, 0.11);
        OutputConstraint con3 = optPlan.addConstraint("maxthick", OutputConstraint.ATTRIBUTE_LOWER_BOUND, 0.1);
        OutputConstraint con4 = optPlan.addConstraint("maxthick", OutputConstraint.ATTRIBUTE_UPPER_BOUND, 0.15);
        OutputConstraint con5 = optPlan.addConstraint("Cl", OutputConstraint.ATTRIBUTE_LOWER_BOUND, 1.15);
        OutputConstraint con6 = optPlan.addConstraint("Cl", OutputConstraint.ATTRIBUTE_UPPER_BOUND, 1.4);

        Objective obj = optPlan.addObjective("k", Objective.MINIMIZE);


        optPlan.store(myTask);

        ComponentAPI outAPI = out.getAPI();
        String pgm2 = "// DATA EXCHANGE PROGRAM - DO NOT EDIT THIS COMMENT\n" +
                "//parameter \"beamthick\" as beamthick\n" +
                "//parameter \"Cd\" as Cd\n" +
                "//parameter \"Cl\" as Cl\n" +
                "//parameter \"maxthick\" as maxthick\n" +
                "//parameter \"xc\" as xc\n" +
                "//END COMMENT\n" +
                "\n" +
                "afcharct = new Partitioner(Tool.RANDOM, new FileExchanger(C_,  Exchanger.GET, \"afcharct.dat\", null), null);\n" +
                "afcharct.word(new LineLocator(0, new StringLocator(\"maxthick,xc\", Locator.SOP)), 3).read(maxthick);\n" +
                "afcharct.word(new LineLocator(0, new StringLocator(\"maxthick,xc\", Locator.SOP)), 4).read(xc);\n" +
                "afcharct.word(new LineLocator(0, new StringLocator(\"beamthick,xbeam\", Locator.SOP)), 3).read(beamthick);\n" +
                "Cl2 = new Partitioner(Tool.RANDOM, new FileExchanger(C_,  Exchanger.GET,  \"Cl_polar.dat\", null), null);\n" +
                "Cl2.word(new LineLocator(4, new StringLocator(\"alpha\", Locator.SOP)), 2).read(Cl);\n" +
                "Cl2.word(new LineLocator(4, new StringLocator(\"alpha\", Locator.SOP)), 3).read(Cd);\n";
        outAPI.set("program", pgm2);
        outAPI.apply();

        //
        List<DtVariable> varToMap = new ArrayList<DtVariable>();
        varToMap.add(var);
        varToMap.add(v_down01);
        varToMap.add(v_down02);
        varToMap.add(v_down03);
        varToMap.add(v_down04);
        varToMap.add(v_down05);
        varToMap.add(v_up01);
        varToMap.add(v_up02);
        varToMap.add(v_up03);
        varToMap.add(v_up04);
        varToMap.add(v_up05);
        varToMap.add(Cl);
        varToMap.add(Cd);
        varToMap.add(beamthick);
        varToMap.add(maxthick);
        DtUtils.mapAddedParameterList(varToMap);

        //Save the model
        // To verify what you have created, go load this into the Isight Gateway.

        String newName = "MySimOptModel.zmf";
        OutputStream savedFile = new FileOutputStream(newName);
        mgr.saveZippedModel(savedFile);
    }

    //=========================================================================

    /**
     * Method createMySimOptModelFromScratch
     *
     * @throws Exception
     */
    //=========================================================================
    private static void createMySimCodeOptModelFromScratch()
            throws Exception {
        DtModelManager mgr = DtModelManager.createEmptyModel();
        mgr.getModelProperties().setModelName("My SimOpt Model");

        //
        MetaModel rootMM = MetaModelManager.instance().lookupMetaModel(EsiTypes.OPTIMIZATION_COMPONENT);
        DtComponent myTask = DtModelManager.createComponent(rootMM, "MySimcodeOptimization");
        mgr.setRootComponent(myTask);

        MetaModel simcode = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Simcode");
        DtComponent code = DtModelManager.createComponent(simcode, "My Simcode");
        myTask.addComponent(code);

        // Now add a calculator to the task
        MetaModel calcMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Calculator");
        DtComponent calc = DtModelManager.createComponent(calcMM, "My Calc");
        myTask.addComponent(calc);

        //
        DtControlFlow cf1 = DtModelManager.createControlFlow(null, code);
        DtControlFlow cf2 = DtModelManager.createControlFlow(code, calc);
        DtControlFlow cf3 = DtModelManager.createControlFlow(calc, null);

        myTask.addControlFlow(cf1);
        myTask.addControlFlow(cf2);
        myTask.addControlFlow(cf3);

        //
        ComponentAPI simcodeAPI = code.getAPI();
        PluginAPI localPluginAPI = (PluginAPI)simcodeAPI.get("oscommandapi");
        localPluginAPI.set("command", "ackley.exe");
        localPluginAPI.apply();

        //
        DtScalarVariable v_down01 = AddInputParameterReal("v_down01");
        code.addParameter(v_down01);

        DtScalarVariable v_down02 = AddInputParameterReal("v_down02");
        code.addParameter(v_down02);

        DtScalarVariable v_down03 = AddInputParameterReal("v_down03");
        code.addParameter(v_down03);

        DtScalarVariable v_down04 = AddInputParameterReal("v_down04");
        code.addParameter(v_down04);

        DtScalarVariable v_down05 = AddInputParameterReal("v_down05");
        code.addParameter(v_down05);

        DtScalarVariable v_up01 = AddInputParameterReal("v_up01");
        code.addParameter(v_up01);

        DtScalarVariable v_up02 = AddInputParameterReal("v_up02");
        code.addParameter(v_up02);

        DtScalarVariable v_up03 = AddInputParameterReal("v_up03");
        code.addParameter(v_up03);

        DtScalarVariable v_up04 = AddInputParameterReal("v_up04");
        code.addParameter(v_up04);

        DtScalarVariable v_up05 = AddInputParameterReal("v_up05");
        code.addParameter(v_up05);

        DtScalarVariable localDtScalarVariable3 = DtModelManager.createScalarVariable("AeroIn_txt", "com.engineous.datatype.File", 1, 1, null, null);

        FileValueType localFileValueType = (FileValueType)localDtScalarVariable3.getValueObj();
        localFileValueType.setHandlerType("com.engineous.plugin.DataHandlerFile");
        localFileValueType.getHandler().setResourceName("c:\\tmp\\AeroIn.txt");
        localFileValueType.setToOption(5);
        code.addParameter(localDtScalarVariable3);

        DtPlugin localDtPlugin = (DtPlugin)simcodeAPI.get("inputdataexchange");
        String pgm1 = "// DATA EXCHANGE PROGRAM - DO NOT EDIT THIS COMMENT\n" +
                "//parameter \"v_down01\" as v_down01\n" +
                "//parameter \"v_down02\" as v_down02\n" +
                "//parameter \"v_down03\" as v_down03\n" +
                "//parameter \"v_down04\" as v_down04\n" +
                "//parameter \"v_down05\" as v_down05\n" +
                "//parameter \"v_up01\" as v_up01\n" +
                "//parameter \"v_up02\" as v_up02\n" +
                "//parameter \"v_up03\" as v_up03\n" +
                "//parameter \"v_up04\" as v_up04\n" +
                "//parameter \"v_up05\" as v_up05\n" +
                "//END COMMENT\n" +
                "deform = new Partitioner(Tool.RANDOM, new FileExchanger(C_, Exchanger.PUT, \"deform.d_tmpl\", \"deform.d\"), null);\n" +
                "deform.word(new LineLocator(1, new StringLocator(\"(涓嬬考闈¢)\", Locator.SOP)), 2).write(v_down01);\n" +
                "deform.word(new LineLocator(3, new StringLocator(\"(涓嬬考闈¢)\", Locator.SOP)), 2).write(v_down02);\n" +
                "deform.word(new LineLocator(5, new StringLocator(\"(涓嬬考闈¢)\", Locator.SOP)), 2).write(v_down03);\n" +
                "deform.word(new LineLocator(7, new StringLocator(\"(涓嬬考闈¢)\", Locator.SOP)), 2).write(v_down04);\n" +
                "deform.word(new LineLocator(9, new StringLocator(\"(涓嬬考闈¢)\", Locator.SOP)), 2).write(v_down05);\n" +
                "deform.word(new LineLocator(1, new StringLocator(\"(涓婄考闈¢)\", Locator.SOP)), 2).write(v_up01);\n" +
                "deform.word(new LineLocator(3, new StringLocator(\"(涓婄考闈¢)\", Locator.SOP)), 2).write(v_up02);\n" +
                "deform.word(new LineLocator(5, new StringLocator(\"(涓婄考闈¢)\", Locator.SOP)), 2).write(v_up03);\n" +
                "deform.word(new LineLocator(7, new StringLocator(\"(涓婄考闈¢)\", Locator.SOP)), 2).write(v_up04);\n" +
                "deform.word(new LineLocator(9, new StringLocator(\"(涓婄考闈¢)\", Locator.SOP)), 2).write(v_up05);\n";
        ((ScalarVariable)localDtPlugin.getProperty("program")).getValueObj().setValue(pgm1);


        DtPlugin localDtPluginOut = (DtPlugin)simcodeAPI.get("outputdataexchange");
        String pgm2 = "// DATA EXCHANGE PROGRAM - DO NOT EDIT THIS COMMENT\n" +
                "//parameter \"beamthick\" as beamthick\n" +
                "//parameter \"Cd\" as Cd\n" +
                "//parameter \"Cl\" as Cl\n" +
                "//parameter \"maxthick\" as maxthick\n" +
                "//parameter \"xc\" as xc\n" +
                "//END COMMENT\n" +
                "\n" +
                "afcharct = new Partitioner(Tool.RANDOM, new FileExchanger(C_,  Exchanger.GET, \"afcharct.dat\", null), null);\n" +
                "afcharct.word(new LineLocator(0, new StringLocator(\"maxthick,xc\", Locator.SOP)), 3).read(maxthick);\n" +
                "afcharct.word(new LineLocator(0, new StringLocator(\"maxthick,xc\", Locator.SOP)), 4).read(xc);\n" +
                "afcharct.word(new LineLocator(0, new StringLocator(\"beamthick,xbeam\", Locator.SOP)), 3).read(beamthick);\n" +
                "Cl2 = new Partitioner(Tool.RANDOM, new FileExchanger(C_,  Exchanger.GET,  \"Cl_polar.dat\", null), null);\n" +
                "Cl2.word(new LineLocator(4, new StringLocator(\"alpha\", Locator.SOP)), 2).read(Cl);\n" +
                "Cl2.word(new LineLocator(4, new StringLocator(\"alpha\", Locator.SOP)), 3).read(Cd);\n";
        ((ScalarVariable)localDtPluginOut.getProperty("program")).getValueObj().setValue(pgm2);

        simcodeAPI.apply();

        ComponentAPI calculatorAPI = calc.getAPI();
        String calcExp = "k=Cl/Cd";
        calculatorAPI.set("expression", calcExp);
        calculatorAPI.apply();

        //Save the model
        // To verify what you have created, go load this into the Isight Gateway.

        String newName = "MySimCodeOptModel.zmf";
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

    private static DtComponent createSimcodeComponent(String name) throws Exception
    {
        MetaModel simcode = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Simcode");
        DtComponent code = DtModelManager.createComponent(simcode, name);

        return code;
    }

    private static void setSimcodeCommand(DtComponent code, String cmd, String args) throws Exception
    {
        //
        ComponentAPI simcodeAPI = code.getAPI();
        PluginAPI localPluginAPI = (PluginAPI)simcodeAPI.get("oscommandapi");
        localPluginAPI.set("command", cmd + " " + args);
        localPluginAPI.apply();
    }

    //=========================================================================

    /**
     * Method createMyOptModelFromScratch
     *
     * @throws Exception
     */
    //=========================================================================
    private static void createMyWingDOptModelFromScratch()
            throws Exception {
        DtModelManager mgr = DtModelManager.createEmptyModel();
        mgr.getModelProperties().setModelName("My WingD Optimization Model");

        //
        MetaModel rootMM = MetaModelManager.instance().lookupMetaModel(EsiTypes.OPTIMIZATION_COMPONENT);
        DtComponent myTask = DtModelManager.createComponent(rootMM, "MyOptimization");
        mgr.setRootComponent(myTask);

        // Now add a simcode to the task
        DtComponent sm = createSimcodeComponent("blwf");
        myTask.addComponent(sm);

        //
        DtControlFlow cf1 = DtModelManager.createControlFlow(null, sm);
        DtControlFlow cf2 = DtModelManager.createControlFlow(sm, null);

        myTask.addControlFlow(cf1);
        myTask.addControlFlow(cf2);

        //
        DtScalarVariable var = DtModelManager.createScalarVariable("x", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                Variable.MODE_INPUT, null, null);

        var.getValueObj().setValue(100);
        sm.addParameter(var);

        DtUtils.copyParameters(sm, myTask);

        DtScalarVariable var2 = DtModelManager.createScalarVariable("y", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                Variable.MODE_OUTPUT, null, null);

        sm.addParameter(var2);

        //
        OptimizationPlan optPlan = DesignDriverFactory.createOptPlan(myTask);
        DesignVariable dv = optPlan.addDesignVariable("x");
        Objective obj = optPlan.addObjective("y", Objective.MINIMIZE);
        optPlan.store(myTask);

        //
        setSimcodeCommand(sm, "E:\\20130821\\WingOpti20130826\\bin\\ComPar.exe",
                "E:\\20130821\\WingOpti20130826\\project1001\\WF.inp");

        ComponentAPI simcodeAPI = sm.getAPI();
        DtPlugin localDtPlugin = (DtPlugin)simcodeAPI.get("inputdataexchange");
        DtPlugin localDtPluginOut = (DtPlugin)simcodeAPI.get("outputdataexchange");


        //
        List<DtVariable> varToMap = new ArrayList<DtVariable>();
        varToMap.add(var);
        DtUtils.mapAddedParameterList(varToMap);

        //Save the model
        // To verify what you have created, go load this into the Isight Gateway.

        String newName = "MyWingDOptModel.zmf";
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

