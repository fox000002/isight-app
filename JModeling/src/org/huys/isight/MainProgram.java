package org.huys.isight;

public class MainProgram {
    public static void main(String[] args) {
        System.out.println("Isight Application");

        String[] params = {"MyOptModelScriptDynamicJavaExec.zmf"};

        //
        System.out.println("Creating Isight Model");
        Modelling.run(params);

        //
        System.out.println("Running BackendModeling");
        BackendModeling.run(params);

        //
        ModellingExample.createModelWithCalc();
        ModellingExample.createModelWithScript();
        ModellingExample.createModelWithScriptDynamicJava();
        ModellingExample.createModelWithScriptDynamicJavaExec();


        //System.out.println("Executing Isight Model");
        //ModelExecution.run(params);
    }

}