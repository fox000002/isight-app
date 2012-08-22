package org.huys.isight;

public class MainProgram {
    public static void main(String[] args) {
        System.out.println("Isight Application");

        String[] params = {"mycase.zmf"};

        ModelExecution.run(params);

        //Modelling.run(params);
    }

}