package org.huys.isight;

import com.engineous.sdk.component.ComponentAPI;
import com.engineous.sdk.designdriver.DesignDriverFactory;
import com.engineous.sdk.designdriver.parameters.DesignVariable;
import com.engineous.sdk.designdriver.parameters.Objective;
import com.engineous.sdk.designdriver.plan.OptimizationPlan;
import com.engineous.sdk.designdriver.techniques.OptimizationTechnique;
import com.engineous.sdk.log.Log;
import com.engineous.sdk.log.SysLog;
import com.engineous.sdk.metamodel.MetaModel;
import com.engineous.sdk.metamodel.MetaModelManager;
import com.engineous.sdk.model.*;
import com.engineous.sdk.server.Logon;
import com.engineous.sdk.vars.EsiTypes;
import com.engineous.sdk.vars.Variable;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: huys03@hotmail.com
 * Date: 2013-2-21
 * Time: 7:25pm
 * To create a function optimization model with different components
 */
public class ModellingExample {
    /**
     * Method createModelWithCalc
     */
    public static void createModelWithCalc() {
        try {
            // Must make Library connection
            Logon.initStandalone("ModellingExample with Calc");

            //
            DtModelManager mgr = DtModelManager.createEmptyModel();
            mgr.getModelProperties().setModelName("Ackley Function Optimization Model");

            //
            MetaModel rootMM = MetaModelManager.instance().lookupMetaModel(EsiTypes.OPTIMIZATION_COMPONENT);
            DtComponent myTask = DtModelManager.createComponent(rootMM, "MyOptimization");
            mgr.setRootComponent(myTask);

            // Now add a calculator to the task
            MetaModel calcMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Calculator");
            DtComponent calc = DtModelManager.createComponent(calcMM, "MyCalc");
            myTask.addComponent(calc);

            //
            DtControlFlow cf1 = DtModelManager.createControlFlow(null, calc);
            DtControlFlow cf2 = DtModelManager.createControlFlow(calc, null);

            myTask.addControlFlow(cf1);
            myTask.addControlFlow(cf2);

            //
            DtScalarVariable var = DtModelManager.createScalarVariable("y", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                    Variable.MODE_OUTPUT, null, null);
            calc.addParameter(var);

            DtScalarVariable var2 = DtModelManager.createScalarVariable("x1", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                     Variable.MODE_INOUT, null, null);
            var2.getValueObj().setValue(2.0);
            calc.addParameter(var2);

            DtScalarVariable var3 = DtModelManager.createScalarVariable("x2", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                    Variable.MODE_INOUT, null, null);
            var3.getValueObj().setValue(2.0);
            calc.addParameter(var3);

            DtUtils.copyParameters(calc, myTask);

            //
            List<DtVariable> varToMap = new ArrayList<DtVariable>();
            varToMap.add(var);
            varToMap.add(var2);
            varToMap.add(var3);
            DtUtils.mapAddedParameterList(varToMap);

            //
            ComponentAPI calculatorAPI = calc.getAPI();
            String calc1 = "c1=20\n"
                     + "c2=0.2\n"
                    + "c3=2*pi()\n"
                    + "y=-c1*exp(-c2*sqrt((x1^2+x2^2)/2))-exp((cos(c3*x1)+cos(c3*x2))/2)+c1+e()";
            calculatorAPI.set("expression", calc1);
            calculatorAPI.apply();

            OptimizationPlan optPlan = DesignDriverFactory.createOptPlan(myTask);
            optPlan.setTechnique("com.engineous.plugin.optimization.Miga");

            DesignVariable dv1 = optPlan.addDesignVariable("x1");
            dv1.setBound(DesignVariable.LOWER_BOUND, -5.0);
            dv1.setBound(DesignVariable.UPPER_BOUND, 5.0);
            DesignVariable dv2 = optPlan.addDesignVariable("x2");
            dv2.setBound(DesignVariable.LOWER_BOUND, -5.0);
            dv2.setBound(DesignVariable.UPPER_BOUND, 5.0);

            Objective obj = optPlan.addObjective("y", Objective.MINIMIZE);
            optPlan.store(myTask);

            //Save the model
            // To verify what you have created, go load this into the Isight Gateway.
            String newName = "MyOptModelCalc.zmf";
            OutputStream savedFile = new FileOutputStream(newName);
            mgr.saveZippedModel(savedFile);

        } catch (Exception e) {
            SysLog.log(Log.ERROR, e, "Exception caught in CreateModelWithCalc...");
        }
    }

    /**
     * Method createModelWithScript
     */
    public static void createModelWithScript() {
        try {
            // Must make Library connection
            Logon.initStandalone("ModellingExample with Script");

            //
            DtModelManager mgr = DtModelManager.createEmptyModel();
            mgr.getModelProperties().setModelName("Ackley Function Optimization Model");

            //
            MetaModel rootMM = MetaModelManager.instance().lookupMetaModel(EsiTypes.OPTIMIZATION_COMPONENT);
            DtComponent myTask = DtModelManager.createComponent(rootMM, "MyOptimization");
            mgr.setRootComponent(myTask);

            // Now add a calculator to the task
            MetaModel scriptMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Script");
            DtComponent script = DtModelManager.createComponent(scriptMM, "MyScript");
            myTask.addComponent(script);

            //
            DtControlFlow cf1 = DtModelManager.createControlFlow(null, script);
            DtControlFlow cf2 = DtModelManager.createControlFlow(script, null);

            myTask.addControlFlow(cf1);
            myTask.addControlFlow(cf2);

            //
            DtScalarVariable var = DtModelManager.createScalarVariable("y", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                    Variable.MODE_OUTPUT, null, null);
            script.addParameter(var);

            DtScalarVariable var2 = DtModelManager.createScalarVariable("x1", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                    Variable.MODE_INOUT, null, null);
            var2.getValueObj().setValue(2.0);
            script.addParameter(var2);

            DtScalarVariable var3 = DtModelManager.createScalarVariable("x2", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                    Variable.MODE_INOUT, null, null);
            var3.getValueObj().setValue(2.0);
            script.addParameter(var3);

            DtUtils.copyParameters(script, myTask);

            //
            List<DtVariable> varToMap = new ArrayList<DtVariable>();
            varToMap.add(var);
            varToMap.add(var2);
            varToMap.add(var3);
            DtUtils.mapAddedParameterList(varToMap);

            //
            ComponentAPI scriptAPI = script.getAPI();
            scriptAPI.set("scriptingLanguage", "Jython");
            String lang = scriptAPI.getString("scriptingLanguage");
            System.out.println("scriptLanguage: "+ lang);

            String script1 = String.format( "print 'myscript runs'\n"
                    + "from math import pi,e,exp,sqrt,cos\n"
                    + "c1=20\n"
                    + "c2=0.2\n"
                    + "c3=2*pi\n"
                    + "%s=-c1*exp(-c2*sqrt((%s*%s+%s*%s/2)))-exp((cos(c3*%s)+cos(c3*%s))/2)+c1+e\n"
                    + "jobLog.logInfo(\"y :\" + str(%s)) ",
                    scriptAPI.getString("varName", "y"),
                    scriptAPI.getString("varName", "x1"),
                    scriptAPI.getString("varName", "x1"),
                    scriptAPI.getString("varName", "x2"),
                    scriptAPI.getString("varName", "x2"),
                    scriptAPI.getString("varName", "x1"),
                    scriptAPI.getString("varName", "x2"),
                    scriptAPI.getString("varName", "y")
             );
            scriptAPI.set("script", script1);
            scriptAPI.apply();

            OptimizationPlan optPlan = DesignDriverFactory.createOptPlan(myTask);
            optPlan.setTechnique("com.engineous.plugin.optimization.Miga");

            DesignVariable dv1 = optPlan.addDesignVariable("x1");
            dv1.setBound(DesignVariable.LOWER_BOUND, -5.0);
            dv1.setBound(DesignVariable.UPPER_BOUND, 5.0);
            DesignVariable dv2 = optPlan.addDesignVariable("x2");
            dv2.setBound(DesignVariable.LOWER_BOUND, -5.0);
            dv2.setBound(DesignVariable.UPPER_BOUND, 5.0);

            Objective obj = optPlan.addObjective("y", Objective.MINIMIZE);
            optPlan.store(myTask);

            //Save the model
            // To verify what you have created, go load this into the Isight Gateway.
            String newName = "MyOptModelScript.zmf";
            OutputStream savedFile = new FileOutputStream(newName);
            mgr.saveZippedModel(savedFile);
        } catch (Exception e) {
            SysLog.log(Log.ERROR, e, "Exception caught in CreateModelWithScript...");
        }
    }

    /**
     * Method createModelWithScriptDynamicJava
     */
    public static void createModelWithScriptDynamicJava() {
        try {
            // Must make Library connection
            Logon.initStandalone("ModellingExample with Script DynamicJava");

            //
            DtModelManager mgr = DtModelManager.createEmptyModel();
            mgr.getModelProperties().setModelName("Ackley Function Optimization Model");

            //
            MetaModel rootMM = MetaModelManager.instance().lookupMetaModel(EsiTypes.OPTIMIZATION_COMPONENT);
            DtComponent myTask = DtModelManager.createComponent(rootMM, "MyOptimization");
            mgr.setRootComponent(myTask);

            // Now add a calculator to the task
            MetaModel scriptMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Script");
            DtComponent script = DtModelManager.createComponent(scriptMM, "MyScript");
            myTask.addComponent(script);

            //
            DtControlFlow cf1 = DtModelManager.createControlFlow(null, script);
            DtControlFlow cf2 = DtModelManager.createControlFlow(script, null);

            myTask.addControlFlow(cf1);
            myTask.addControlFlow(cf2);

            //
            DtScalarVariable var = DtModelManager.createScalarVariable("y", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                    Variable.MODE_OUTPUT, null, null);
            script.addParameter(var);

            DtScalarVariable var2 = DtModelManager.createScalarVariable("x1", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                    Variable.MODE_INOUT, null, null);
            var2.getValueObj().setValue(2.0);
            script.addParameter(var2);

            DtScalarVariable var3 = DtModelManager.createScalarVariable("x2", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                    Variable.MODE_INOUT, null, null);
            var3.getValueObj().setValue(2.0);
            script.addParameter(var3);

            DtUtils.copyParameters(script, myTask);

            //
            List<DtVariable> varToMap = new ArrayList<DtVariable>();
            varToMap.add(var);
            varToMap.add(var2);
            varToMap.add(var3);
            DtUtils.mapAddedParameterList(varToMap);

            //
            ComponentAPI scriptAPI = script.getAPI();
            scriptAPI.set("scriptingLanguage", "DynamicJava");
            String lang = scriptAPI.getString("scriptingLanguage");
            System.out.println("scriptLanguage: "+ lang);

            String script1 = String.format( "jobLog.logInfo(\"myscript runs\");\n"
                    + "c1=20;\n"
                    + "c2=0.2;\n"
                    + "c3=2*Math.PI;\n"
                    + "%s=-c1*Math.exp(-c2*Math.sqrt((%s*%s+%s*%s/2)))-Math.exp((Math.cos(c3*%s)+Math.cos(c3*%s))/2)+c1+Math.E;\n"
                    + "jobLog.logInfo(\"y :\" + %s); ",
                    scriptAPI.getString("varName", "y"),
                    scriptAPI.getString("varName", "x1"),
                    scriptAPI.getString("varName", "x1"),
                    scriptAPI.getString("varName", "x2"),
                    scriptAPI.getString("varName", "x2"),
                    scriptAPI.getString("varName", "x1"),
                    scriptAPI.getString("varName", "x2"),
                    scriptAPI.getString("varName", "y")
            );


            scriptAPI.set("script", script1);
            scriptAPI.apply();

            OptimizationPlan optPlan = DesignDriverFactory.createOptPlan(myTask);
            optPlan.setTechnique("com.engineous.plugin.optimization.Miga");

            DesignVariable dv1 = optPlan.addDesignVariable("x1");
            dv1.setBound(DesignVariable.LOWER_BOUND, -5.0);
            dv1.setBound(DesignVariable.UPPER_BOUND, 5.0);
            DesignVariable dv2 = optPlan.addDesignVariable("x2");
            dv2.setBound(DesignVariable.LOWER_BOUND, -5.0);
            dv2.setBound(DesignVariable.UPPER_BOUND, 5.0);

            Objective obj = optPlan.addObjective("y", Objective.MINIMIZE);
            optPlan.store(myTask);

            //Save the model
            // To verify what you have created, go load this into the Isight Gateway.
            String newName = "MyOptModelScriptDynamicJava.zmf";
            OutputStream savedFile = new FileOutputStream(newName);
            mgr.saveZippedModel(savedFile);
        } catch (Exception e) {
            SysLog.log(Log.ERROR, e, "Exception caught in CreateModelWithScript...");
        }
    }

    /**
     * Method createModelWithScriptDynamicJavaExec
     */
    public static void createModelWithScriptDynamicJavaExec() {
        try {
            // Must make Library connection
            Logon.initStandalone("ModellingExample with Script DynamicJava");

            //
            DtModelManager mgr = DtModelManager.createEmptyModel();
            mgr.getModelProperties().setModelName("Ackley Function Optimization Model");

            //
            MetaModel rootMM = MetaModelManager.instance().lookupMetaModel(EsiTypes.OPTIMIZATION_COMPONENT);
            DtComponent myTask = DtModelManager.createComponent(rootMM, "MyOptimization");
            mgr.setRootComponent(myTask);

            // Now add a calculator to the task
            MetaModel scriptMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Script");
            DtComponent script = DtModelManager.createComponent(scriptMM, "MyScript");
            myTask.addComponent(script);

            //
            DtControlFlow cf1 = DtModelManager.createControlFlow(null, script);
            DtControlFlow cf2 = DtModelManager.createControlFlow(script, null);

            myTask.addControlFlow(cf1);
            myTask.addControlFlow(cf2);

            //
            DtScalarVariable var = DtModelManager.createScalarVariable("y", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                    Variable.MODE_OUTPUT, null, null);
            script.addParameter(var);

            DtScalarVariable var2 = DtModelManager.createScalarVariable("x1", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                    Variable.MODE_INOUT, null, null);
            var2.getValueObj().setValue(2.0);
            script.addParameter(var2);

            DtScalarVariable var3 = DtModelManager.createScalarVariable("x2", EsiTypes.REAL, Variable.ROLE_PARAMETER,
                    Variable.MODE_INOUT, null, null);
            var3.getValueObj().setValue(2.0);
            script.addParameter(var3);

            DtUtils.copyParameters(script, myTask);

            //
            List<DtVariable> varToMap = new ArrayList<DtVariable>();
            varToMap.add(var);
            varToMap.add(var2);
            varToMap.add(var3);
            DtUtils.mapAddedParameterList(varToMap);

            //
            ComponentAPI scriptAPI = script.getAPI();
            scriptAPI.set("scriptingLanguage", "DynamicJava");
            String lang = scriptAPI.getString("scriptingLanguage");
            System.out.println("scriptLanguage: "+ lang);

            String script1 = String.format("import java.io.BufferedWriter;\n" +
                    "import java.io.FileWriter;\n" +
                    "import java.util.Scanner;\n" +
                    "str= %s + \"  \" + %s ;\n" +
                    "try\n" +
                    "{\n" +
                    "BufferedWriter bw = new BufferedWriter(new FileWriter(localDir + \"input.dat\"));\n" +
                    "bw.write(str);\n" +
                    "bw.close();\n" +
                    "rt = Runtime.getRuntime() ;\n" +
                    "out = new File(localDir + \"output.dat\");\n" +
                    "rt.exec(\"ackley.exe \" + localDir + \"input.dat \" + localDir + \"output.dat\") ;\n" +
                    "while(true){\n" +
                    "if (out.exists()) {\n" +
                    "Scanner scan = new Scanner(out);\n" +
                    "%s = (scan.nextDouble());\n" +
                    "scan.close();\n" +
                    "break;\n" +
                    "}\n" +
                    "}\n" +
                    "} catch (Exception ex) {\n" +
                    "jobLog.logInfo(ex.getMessage());\n" +
                    "}\n ",
                    scriptAPI.getString("varName", "x1"),
                    scriptAPI.getString("varName", "x2"),
                    scriptAPI.getString("varName", "y")
            );
            scriptAPI.set("script", script1);
            scriptAPI.apply();

            OptimizationPlan optPlan = DesignDriverFactory.createOptPlan(myTask);
            optPlan.setTechnique("com.engineous.plugin.optimization.Miga");

            // Change the technique option
            OptimizationTechnique tech = (OptimizationTechnique) optPlan.getTechnique();
            tech.getOption("Number of Generations").setValue(100);

            DesignVariable dv1 = optPlan.addDesignVariable("x1");
            dv1.setBound(DesignVariable.LOWER_BOUND, -5.0);
            dv1.setBound(DesignVariable.UPPER_BOUND, 5.0);
            DesignVariable dv2 = optPlan.addDesignVariable("x2");
            dv2.setBound(DesignVariable.LOWER_BOUND, -5.0);
            dv2.setBound(DesignVariable.UPPER_BOUND, 5.0);

            Objective obj = optPlan.addObjective("y", Objective.MINIMIZE);
            optPlan.store(myTask);

            //Save the model
            // To verify what you have created, go load this into the Isight Gateway.
            String newName = "MyOptModelScriptDynamicJavaExec.zmf";
            OutputStream savedFile = new FileOutputStream(newName);
            mgr.saveZippedModel(savedFile);
        } catch (Exception e) {
            SysLog.log(Log.ERROR, e, "Exception caught in CreateModelWithScript...");
        }
    }
}