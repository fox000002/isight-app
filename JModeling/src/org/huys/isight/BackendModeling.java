package org.huys.isight;

import com.engineous.sdk.log.Log;
import com.engineous.sdk.log.SysLog;
import com.engineous.sdk.metamodel.MetaModel;
import com.engineous.sdk.metamodel.MetaModelManager;
import com.engineous.sdk.model.DtComponent;
import com.engineous.sdk.model.DtControlFlow;
import com.engineous.sdk.model.DtModelManager;
import com.engineous.sdk.server.Logon;
import com.engineous.sdk.vars.EsiTypes;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by hu on 2014/8/28.
 */
public class BackendModeling {
    //=========================================================================

    /**
     * Main routine runs the tests.
     *
     * @param args
     */
    public static void run(String[] args) {

        try {
            // Must make Library connection
            Logon.initStandalone("Backend Modeling");

            System.out.println("calling createDOEAndApproximationModel()");
            createDOEAndApproximationModel();

            System.out.println("calling createDOEAndMonteCarloModel()");
            createDOEAndMonteCarloModel();

        } catch (Exception e) {
            SysLog.log(Log.ERROR, e, "Exception caught in BackendModeling...");
        }

    }

    public static void createDOEAndApproximationModel() throws Exception {
        DtModelManager mgr = DtModelManager.createEmptyModel();
        mgr.getModelProperties().setModelName("My DOE and Approximation Model");

        //
        MetaModel taskMM = MetaModelManager.instance().lookupMetaModel(EsiTypes.TASK_COMPONENT);

        DtComponent task = DtModelManager.createComponent(taskMM, "My root task");

        DtComponent approx = DtModelManager.createComponent("com.engineous.component.Approximation", "Approx");

        MetaModel doeMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.DOE");
        DtComponent doe = DtModelManager.createComponent(doeMM, "DOE");

        MetaModel simCodeMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Simcode");
        DtComponent ug = DtModelManager.createComponent(simCodeMM, "UG");
        DtComponent ansys = DtModelManager.createComponent(simCodeMM, "ANSYS");
        DtComponent np = DtModelManager.createComponent(simCodeMM, "NP");

        mgr.setRootComponent(task);
        task.addComponent(doe);
        task.addComponent(approx);

        DtControlFlow xcf1 = DtModelManager.createControlFlow(null, doe);
        DtControlFlow xcf2 = DtModelManager.createControlFlow(doe, approx);
        DtControlFlow xcf3 = DtModelManager.createControlFlow(approx, null);

        task.addControlFlow(xcf1);
        task.addControlFlow(xcf2);
        task.addControlFlow(xcf3);

        doe.addComponent(ug);
        doe.addComponent(ansys);
        doe.addComponent(np);


        //
        DtControlFlow cf1 = DtModelManager.createControlFlow(null, ug);
        DtControlFlow cf2 = DtModelManager.createControlFlow(ug, ansys);
        DtControlFlow cf3 = DtModelManager.createControlFlow(ansys, np);
        DtControlFlow cf4 = DtModelManager.createControlFlow(np, null);

        doe.addControlFlow(cf1);
        doe.addControlFlow(cf2);
        doe.addControlFlow(cf3);
        doe.addControlFlow(cf4);


        //Save the model
        // To verify what you have created, go load this into the Isight Gateway.
        String newName = "MyDOEAndApproximationModel.zmf";
        OutputStream savedFile = new FileOutputStream(newName);
        mgr.saveZippedModel(savedFile);
    }

    public static void createDOEAndMonteCarloModel() throws Exception {
        DtModelManager mgr = DtModelManager.createEmptyModel();
        mgr.getModelProperties().setModelName("My DOE and Monte Carlo Model");

        //
        MetaModel mcMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.MonteCarlo");
        DtComponent mc = DtModelManager.createComponent(mcMM, "MonteCarlo");

        MetaModel doeMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.DOE");
        DtComponent doe = DtModelManager.createComponent(doeMM, "DOE");

        MetaModel simCodeMM = MetaModelManager.instance().lookupMetaModel("com.engineous.component.Simcode");
        DtComponent ug = DtModelManager.createComponent(simCodeMM, "UG");
        DtComponent ansys = DtModelManager.createComponent(simCodeMM, "ANSYS");
        DtComponent np = DtModelManager.createComponent(simCodeMM, "NP");

        mgr.setRootComponent(mc);

        mc.addComponent(doe);


        DtControlFlow xcf1 = DtModelManager.createControlFlow(null, doe);
        DtControlFlow xcf2 = DtModelManager.createControlFlow(doe, null);
        mc.addControlFlow(xcf1);
        mc.addControlFlow(xcf2);

        doe.addComponent(ug);
        doe.addComponent(ansys);
        doe.addComponent(np);


        DtControlFlow cf1 = DtModelManager.createControlFlow(null, ug);
        DtControlFlow cf2 = DtModelManager.createControlFlow(ug, ansys);
        DtControlFlow cf3 = DtModelManager.createControlFlow(ansys, np);
        DtControlFlow cf4 = DtModelManager.createControlFlow(np, null);

        doe.addControlFlow(cf1);
        doe.addControlFlow(cf2);
        doe.addControlFlow(cf3);
        doe.addControlFlow(cf4);


        //Save the model
        // To verify what you have created, go load this into the Isight Gateway.
        String newName = "MyDOEAndMonteCarloModel.zmf";
        OutputStream savedFile = new FileOutputStream(newName);
        mgr.saveZippedModel(savedFile);
    }
}
