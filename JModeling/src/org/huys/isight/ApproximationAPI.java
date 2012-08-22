package org.huys.isight;

import com.engineous.sdk.component.ComponentAPI;
import com.engineous.sdk.exception.SDKException;
import com.engineous.sdk.model.DtComponent;
import com.engineous.sdk.model.DtModelManager;
import com.engineous.sdk.model.DtScalarVariable;
import com.engineous.sdk.model.exceptions.DtModelException;
import com.engineous.sdk.pse.LogonFailedException;
import com.engineous.sdk.server.Logon;
import com.engineous.sdk.vars.EsiTypes;
import com.engineous.sdk.vars.Variable;
import com.engineous.sdk.vars.VariableException;

//=========================================================================

/**
 * Title:        ApproxExample <br>
 * Description:  $defaultDesc$<br>
 * Copyright:    $copyright$<br>
 * Company:      $company$<br>
 *
 * @author Enter your name here...
 */
//=========================================================================
public class ApproximationAPI {

    /**
     * In this example, we demonstrate how to create approximations purely using
     * the API. We will first create an approximation component along with its
     * parameters. The approximation technique, along with its options will then
     * be set. The approximation will finally be initialized using a data file.
     *
     * @param args
     * @throws LogonFailedException
     * @throws DtModelException
     * @throws SDKException
     * @throws VariableException
     */
    public static void main(String[] args)
            throws SDKException, LogonFailedException, DtModelException, VariableException {

        //Connect to a library
        Logon.initStandalone();

        // Step 1
        // Create the approximation component with a name, say
        // "FirstApprox"
        DtComponent comp = DtModelManager.createComponent("com.engineous.component.Approximation", "FirstApprox");
        System.out.println("Component created.");

        // Create a model - a model needs to be created in order to obtain
        // the API class
        DtModelManager.createModel(comp);
        System.out.println("Model created.");

        // Get the API class
        ComponentAPI compAPI = comp.getAPI();
        // Create the parameters for the component.
        DtScalarVariable in1 = DtModelManager.createScalarVariable("x1", EsiTypes.REAL, Variable.ROLE_PARAMETER, Variable.MODE_INPUT, null, null);
        DtScalarVariable in2 = DtModelManager.createScalarVariable("x2", EsiTypes.REAL, Variable.ROLE_PARAMETER, Variable.MODE_INPUT, null, null);
        DtScalarVariable out = DtModelManager.createScalarVariable("y1", EsiTypes.REAL, Variable.ROLE_PARAMETER, Variable.MODE_OUTPUT, null, null);
        // Add the parameters to the component
        comp.addParameter(in1);
        comp.addParameter(in2);
        comp.addParameter(out);
        System.out.println("Created and added parameters to the component.");

        // Step 2 - Create and configure the approximation technique
        // Set the approximation technique
        compAPI.set("ApproximationTechniqueName", "RBF Model");

        // Specify the input and output parameters. Remember, these parameters
        // should be available in the approximation component.
        compAPI.set("InputParameterNames", new String[]{"x1", "x2"});
        compAPI.set("OutputParameterNames", new String[]{"y1"});

        // Set the technique options
        compAPI.set("ApproximationTechniqueOption", "SmoothingFilter", "5E-2");
        System.out.println("Configured RBF approximation.");

        // Step 3 - Indicate the data file that should be used for initialization
        // 1 - file with sampling points; 3 - file with coefficient data
        compAPI.set("DataFileType", 1);
        compAPI.set("DataFileFullPathName", "OLH-20-DataPoints.txt");
        // The above options are committed only when the apply() function is
        // called. The apply() function should be called before initializing the
        // approximation. Exceptions will be thrown if the approximation
        // technique is not available in the library or if the library path is
        // not configured properly.
        compAPI.apply();

        // Step 4 - initialize the approximation
        System.out.print("Initializing RBF approximation...");
        compAPI.call("initializeApproximation");
        System.out.println("done.");

        System.out.print("Value of the approximation at (0,0): ");
        // Evaluate the approximation at a point.
        double[] result = (double[]) compAPI.call("evaluate", new double[]{0.0, 0.0});
        for (int i = 0; i < result.length; i++) {
            System.out.print(result[i] + "\t");
        }
        System.out.println();
    }

}

