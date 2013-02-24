package org.huys.isight;

public class MainProgram {
    public static void main(String[] args) {
        System.out.println("Isight Application");

        String[] params = {"mycase.zmf"};

        //
        System.out.println("Creating Isight Model");
        Modelling.run(params);

        //System.out.println("Executing Isight Model");
        //ModelExecution.run(params);

        //
        ModellingExample.createModelWithCalc();
        ModellingExample.createModelWithScript();
    }

}