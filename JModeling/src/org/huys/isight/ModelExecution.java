/* Copyright Dassault Systemes, 1999, 2010 */


package org.huys.isight;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.*;

import com.engineous.sdk.exception.SDKException;
import com.engineous.sdk.frapi.FiperResultSet;
import com.engineous.sdk.frapi.FiperResultSetMetaData;
import com.engineous.sdk.frapi.ResultRequestMgr;
import com.engineous.sdk.frapi.ResultRequestMgrFactory;
import com.engineous.sdk.frapi.VariableDescriptor;
import com.engineous.sdk.frapi.exception.EmptyResultException;
import com.engineous.sdk.frapi.exception.ResultException;
import com.engineous.sdk.log.Log;
import com.engineous.sdk.log.SysLog;
import com.engineous.sdk.model.*;
import com.engineous.sdk.model.exceptions.DtModelException;
import com.engineous.sdk.model.path.DtComponentPathDescriptor;
import com.engineous.sdk.model.path.PathException;
import com.engineous.sdk.pse.JobInfoValue;
import com.engineous.sdk.pse.JobLogValue;
import com.engineous.sdk.pse.PSE;
import com.engineous.sdk.pse.PSEException;
import com.engineous.sdk.pse.PSEUtils;
import com.engineous.sdk.pse.SysPSE;
import com.engineous.sdk.runtime.*;
import com.engineous.sdk.server.Logon;
import com.engineous.sdk.vars.*;

/**
 * Sample code to execute an Isight model.  This main() must
 * be invoked from a startup script that establishes the proper
 * environment.  See the "fipercmd" startup script for an example
 * of how client applications need to be started.
 * <p/>
 * This sample will run a model either in the SIMULIA Execution Engine or
 * on the local machine (Isight), depending on the connection
 * profile (.CPR file) used.  If the connection profile, user-id,
 * and password are not supplied the user is prompted with the usual
 * logon dialog before the model is run.
 * <p/>
 * When the model completes, the result values of the root component's
 * parameters are written to the console in a simple comma-delimited format.
 * This program exits when the model completes execution.
 * <p/>
 * This sample is designed to show the basic APIs and steps necessary to
 * run a model and get results from the job.  To keep the code as simple
 * as possible, extensive error checking and other robust features have
 * been omitted.
 * <p/>
 * Although this supports execution of models in the SIMULIA Execution Engine or locally, for
 * the sake of simplicity it does not support the running of a model stored
 * in the Library.  Only a ZMF model file from the local
 * machine can be executed with this sample.  See the comments below for how
 * to run a model stored in the Library.  There are many other API features
 * that are not used by this example.  See the Development Guide and the
 * javadocs for the API classes for details.
 * <p/>
 * Example usage:
 * <code>
 * execmodel c:\mymodel.zmf
 * </code>
 */
public class ModelExecution {

    //=========================================================================
    public static DtModelManager mgr;
    public static String jobId;
    public static RunInfo runInfo;
    public static int idx = 1;

    /**
     * Method main
     *
     * @param args
     */
    //=========================================================================
    public static void run(String[] args) {

        switch (args.length) {
            case 1:
            case 2:
            case 4:
                break;    // Valid number of args
            default:
                System.out.println("usage: execmodel model-file [connection-profile] [userid] [password]");
                System.exit(1);
        }

        // Setup system properties for logon.  Do not prompt if user ID and password
        // were supplied on the command line.

        if (args.length > 1) {    // Name of CPR file is given
            System.setProperty("fiper.logon.profile", args[1]);
        }

        if (args.length > 2) {    // User id and password are given
            System.setProperty("fiper.logon.prop.user", args[2]);
            System.setProperty("fiper.logon.prop.pw", args[3]);
            System.setProperty("fiper.logon.prompt", "no");
        }

        // Do the logon, quits the JVM if it fails
        Logon.logonFiper(null, "ModelExecution");

        System.out.println("Logged on...");

        try {
            // Prepare job execution details.  There are lots of other
            // job options that can be setup, such a job log level, etc.
            // We just take the defaults for this example.

            runInfo = new RunInfo("my job", true);

            // We load the model from a local ZMF file.  To run a model
            // from the library, just put the name (path) and version of
            // the model into the run options:

            //runInfo.setModelName("samples.MyModel");
            //runInfo.setModelVer(Version.LATEST);

            byte[] modelBytes = loadModelBytes(args[0]);
            //runInfo.setModel(modelBytes);

            mgr = DtModelManager.createModel(new ByteArrayInputStream(modelBytes));

            VariableChangeListener varChangeListener = new VariableChangeListener() {
                @Override
                public void variableChanged(VariableChangeEvent variableChangeEvent) throws VariableException {
                    //To change body of implemented methods use File | Settings | File Templates.
                    System.out.println(variableChangeEvent.getVariable().getDisplayName() + " = "
                            + variableChangeEvent.getValueObj().getAsReal());
                }
            };

            ValueChangeListener valChangeListener = new ValueChangeListener() {

                @Override
                public void valueChanged(ValueChangeEvent valueChangeEvent) throws VariableException {
                    //To change body of implemented methods use File | Settings | File Templates.
                    System.out.println("Value = " + valueChangeEvent.getValue().getAsReal());
                }
            };

            try {
                DtComponent rootComponent = mgr.getRootComponent();

                DtScalarVariable var = (DtScalarVariable) rootComponent.getParameter("x1");

                System.out.println("Var : " + var.getDisplayName() + " = " + var.getValueObj().getAsReal());

                Value v = var.getValueObj();

                var.addVariableChangeListener(varChangeListener);
                v.addValueChangeListener(valChangeListener);

                //v.setValue(200.0);

            } catch (DtModelException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            runInfo.setModel(mgr);

            System.out.println("Creating job...");

            // Get system job management interface
            PSE pse = SysPSE.getPSE();

            // Create the job (but this does not start it, in case we need
            // to do something else before the job starts, let setting up
            // async job event listeners).

            JobInfoValue jobDetails = pse.createJob(runInfo);
            jobId = jobDetails.getId();    // We need this below


            // Here we could setup a listener to get notified of the job's
            // progress, and when it completes.  To keep this example simple
            // and single-threaded, we will just run the job an poll the job
            // status until it is done.
            JobMonitorListener jobListener = new JobMonitorListener() {
                public void jobEvent(JobMonitorEvent event) {
                    if ((event.getEventType() == JobMonitorEvent.TYPE_WORKITEM)
                            && (event.getStatus() == PSEUtils.WORK_DONE)) {

                        System.out.println("Workitem : " + event.getCompName());


                        try {
                            DtComponent rootComponent = mgr.getRootComponent();

                           // RtUtils.writeResults(new DtComponentPath(rootComponent), jobId, new File("test." + idx), false, "\n");

                            idx++;

                            RuntimeEnv env = SysRuntime.getRuntimeEnv();
                            if (env != null) {
                                ScalarVariable dsv = env.getContext().getScalar("x1");
                                if (dsv != null) {
                                    System.out.println(dsv.getValueObj().getAsReal());
                                }
                            }

                            PSE pse = SysPSE.getPSE();

                            JobInfoValue jiv = pse.getJob(jobId, false);

                            WorkItemSummary wis = pse.getJobWorkItemSummary(jobId);

                            int n = wis.getNumDetails();

                            for (int i = 0; i < n; ++i) {
                                WorkItemSummaryDetails wisd = wis.getDetails(i);

                                System.out.println("Item : " + wisd.getCount());
                            }

                            VariableCollection vcc = runInfo.getJobInputList();
                            if (vcc != null) {
                                System.out.println("RunInfo JobInputList");

                                Iterator<Variable> vi = vcc.getIterator();

                                while (vi.hasNext()) {

                                    ScalarVariable sv = (ScalarVariable) vi.next();

                                    System.out.println(sv.getDisplayName() + " : " + sv.getValueObj().getAsString());
                                }


                                //ScalarVariable vx = vcc.getScalar("x1");

                                //if (vx != null) {
                                //    System.out.println("x1 =" + vx.getValueObj().getAsReal());
                                //}
                            }


//                            DtScalarVariable var = (DtScalarVariable)rootComponent.getParameter("x1");
//
//                           //System.out.println("Var : " + var.getDisplayName() + " = " + var.getValueObj().getAsReal());
////
                            DtComponentPathDescriptor rootPathDesc = new DtComponentPathDescriptor(rootComponent);
////                            List<?> rootParmList = rootComponent.getParameterList();
//                            ResultRequestMgr requestMgr = ResultRequestMgrFactory.createInstanceWithAllVariables(jobId, rootPathDesc);
////
//                            requestMgr.getSynchronousResults();
//
//                            FiperResultSet resultSet = requestMgr.getResultSetSnapShot();
//                            FiperResultSetMetaData resultMetaData = (FiperResultSetMetaData) resultSet.getMetaData();
//
//                            RunResultData runresultdata = new RunResultData(rootComponent, resultSet);

                            if (rootComponent.isInstanceOf("com.engineous.system.component.Process")) {
                                Object obj = VariableUtil.getSubflowVariableReferenceList(rootComponent);
                                obj = removeNoSaveDBParametersFromVariableReferenceList(((List) (obj)));
                                ResultRequestMgr resultrequestmgr = ResultRequestMgrFactory.createInstanceWithVarRefs(jobId, rootPathDesc, ((List) (obj)), true);
                                boolean flag1 = resultrequestmgr.isComplete();
                                String s2 = new DtComponentPath(rootComponent).getPathAsString();
                                String s4 = s2;
                                try {
                                    resultrequestmgr.getSynchronousResults();
                                } catch (EmptyResultException emptyresultexception) {
                                }
                                FiperResultSet fiperresultset;
                                for (fiperresultset = resultrequestmgr.getResultSetSnapShot(); !resultrequestmgr.isComplete() && fiperresultset.next(); )
                                    ;

                                FiperResultSet resultSet = fiperresultset;

                                if (null != resultSet) {
                                    FiperResultSetMetaData resultMetaData = (FiperResultSetMetaData) resultSet.getMetaData();
                                    //System.out.println("FiperResultSet");
//
                                    int numRows = resultSet.getNumRows();
                                    //System.out.println("Result Rows : " + numRows);

                                    System.out.println("----------------------------------------------------");
                                    int numColumns = resultMetaData.getColumnCount();
                                    // Now print a line for each row of results in the result set
                                    for (int i = 1; i <= numColumns; i++) {        // Print column headers
                                        //System.out.println(resultMetaData.getColumnName(i));
                                    }
                                    if (resultSet.next()) {
                                        for (int i = 1; i <= numColumns; i++) {    // remember columns are 1-based in SQL
                                            Value val2 = (Value) resultSet.getObject(i);
                                            if (val2 != null) {
                                                //System.out.println(resultMetaData.getColumnName(i) + " : " + val2.getAsString());
                                            }
                                        }
                                    }
                                    System.out.println("----------------------------------------------------");

                                    RunResultData runresultdata = new RunResultData(rootComponent, resultSet);

                                    if (null != runresultdata) {
                                        int nr = runresultdata.getNumRows();
                                        //System.out.println("RunResultData : " + nr);
                                        Variable y = rootComponent.getParameter("y");
                                        Value[] vals = runresultdata.getValueList(y, Variable.MODE_OUTPUT);
                                        if (null != vals) {
                                        for (Value v : vals) {
                                            System.out.println("y : " + v.getAsReal());
                                       }
                                        }
                                    }
                                }
                            }

//
//
//                            if (null != resultSet) {
//                                System.out.println("FiperResultSet");
////
//                                int numRows = resultSet.getNumRows();
//                                System.out.println("Result Rows : " + numRows);
//
//                                System.out.println("----------------------------------------------------");
//                                int numColumns = resultMetaData.getColumnCount();
//                                // Now print a line for each row of results in the result set
//                                for (int i = 1; i <= numColumns; i++) {        // Print column headers
//                                     System.out.println(resultMetaData.getColumnName(i));
//                                }
//                                if (resultSet.next()) {
//                                for (int i = 1; i <= numColumns; i++) {    // remember columns are 1-based in SQL
//                                    Value val2 = (Value) resultSet.getObject(i);
//                                    if (val2 != null) {
//                                     System.out.println(resultMetaData.getColumnName(i) + " : " + val2.getAsString());
//                                    }
//                                }
//                                }
//                                System.out.println("----------------------------------------------------");
//
//
////
//                           }
//
                        } catch (DtModelException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (VariableException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (PathException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (ResultException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (SQLException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (RtException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (PSEException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }

                        WorkItemSummary wis = event.getWorkItemSummary();

                        if (wis != null) {
                            System.out.println("WorkitemSummary: ");
                        }

                        VariableCollection vc = event.getVarCollection();

                        if (vc != null) {

                            try {


                                ScalarVariable vx = null;
                                vx = (ScalarVariable) vc.get("x1");

                                System.out.println("value = " + vx.getValueObj().getAsReal());

                            } catch (VariableException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }


                        }

                    } else if (event.getEventType() == JobMonitorEvent.TYPE_WORKITEM_SUMMARY) {
                        System.out.println("Workitem summary: " + event.getCompName());
                    } else if (event.getEventType() == JobMonitorEvent.TYPE_PARAMETERS_UPDATED) {
                        System.out.println("Parameter update... ");
                    }
                }
            };

            pse.addJobMonitorListener(jobId, jobListener);

            System.out.println("Running job...");
            pse.runJob(jobId);
            jobDetails = waitForJob(jobId, 60);

            if (jobDetails.getStatus() != PSEUtils.JOB_DONE) {
                // Job did not finish in time
                System.out.println("Job " + jobId + " did not complete within 60 seconds.  Cancelling job.");
                pse.cancelJob(jobId);
                waitForJob(jobId, 10);    // Wait a bit longer for cancel to happen
                System.exit(4);
            }

            // Now that job is complete, were there any errors?  If so, print the job log.
            if (jobDetails.getCc() != PSEUtils.JOB_CC_OK) {
                System.out.println("Job " + jobId + " completed with errors (" + PSEUtils.jobCcName(jobDetails.getCc()) + ").");
                @SuppressWarnings("unchecked") Collection<JobLogValue> logList = pse.getLogsForJob(jobId);
                // Each object in the collection is a JobLogValue and represents
                // a single entry in the job log.  They are not necessarily in order.
                // With some work we could sort them (say by time stamp) before display.
                // Added unchecked cast to Collection<JobLogValue> -- may need to be changed
                System.out.println(logList.size() + " job log records.");

                for (JobLogValue logRecord : logList) {
                    // There is a lot of information in the log record, we just print
                    // a few things for this example.
                    System.out.println(logRecord.getMsg().getMessage());
                    if (logRecord.getException() != null) {    // There was an exception associated with this record
                        logRecord.getException().printStackTrace(System.out);
                    }
                }
                System.exit(5);
            }

            // In this example, we get the results ("history") only for the
            // root component of the model.  This could walk the model and get
            // results for all the components.  Getting results for the job
            // requires that we create the Isight model objects (up to this point,
            // this client application holds only the bytes of the model file).
            // This can be an expensive process on a large model.

            System.out.println("Loading model...");
            DtModelManager modelMgr = DtModelManager.createModel(new ByteArrayInputStream(modelBytes));

            // Now get the results for the root component.  By definition the
            // root only runs one time, so we expect only one set of results back.
            // Other components inside of design drivers (DOE, Optimization, etc)
            // may have run many times and return many results.  Note that the "path"
            // of this component through the model is, by definition, just the component
            // name since it is the root.

            DtComponent rootComponent = modelMgr.getRootComponent();

            // This code sets up a request for results for the root component of the model.
            DtComponentPathDescriptor rootPathDesc = new DtComponentPathDescriptor(rootComponent);
            List<?> rootParmList = rootComponent.getParameterList();
            ResultRequestMgr requestMgr = ResultRequestMgrFactory.createInstanceWithVariables(jobId, rootPathDesc, rootParmList);

            // We can get the results in a synchronous call right now, or register a
            // listener and get results asynchronously on another thread.  To keep this
            // example simple, we will just wait for the results.
            System.out.println("Getting results...");
            requestMgr.getSynchronousResults();

            // Results are returned in an extension of the java.sql.ResultSet, so if you
            // know how to process JDBC results, you know how to process Fiper results.
            FiperResultSet resultSet = requestMgr.getResultSetSnapShot();

            FiperResultSetMetaData resultMetaData = (FiperResultSetMetaData) resultSet.getMetaData();

            //Get the number of runs (rows)
            int numRows = resultSet.getNumRows();
            System.out.println("Result Rows : " + numRows);

            // Obtain a Value[] that contains all of the results for a parameter (here
            // we get values for the first parameter (column 1))
            VariableDescriptor varDesc = resultMetaData.getVariableDescriptor(1);
            @SuppressWarnings("unused") Value[] paramValues = resultSet.getValuesForVariable(varDesc);

            // You can also get a single Value if you now the row number and column number of interest:
            @SuppressWarnings("unused") Value val = resultSet.getValue(1, 1);

            //If you know that your result set only contains Real parameters,
            //you can get the results returned in a double[][] using
            //double[][] vals = resultSet.getDoubleTable();


            // We print a simple comma-delimited table format of the results.  Since
            // this is the root component there is only one row, but this will work
            // for multiple rows as well.

            System.out.println("----------------------------------------------------");
            int numColumns = resultMetaData.getColumnCount();
//            for (int i = 1; i <= numColumns; i++) {        // Print column headers
//                System.out.println(resultMetaData.getColumnName(i));
//            }

            // Now print a line for each row of results in the result set
            while (resultSet.next()) {
                for (int i = 1; i <= numColumns; i++) {    // remember columns are 1-based in SQL
                    Value val2 = (Value) resultSet.getObject(i);
                    System.out.println(resultMetaData.getColumnName(i) + " : " + val2.getAsString());
                }
            }
            System.out.println("----------------------------------------------------");

            System.out.println("Done processing job " + jobId);
            System.exit(0);

        } catch (Throwable t) {
            System.out.println("Failed to run job.");
            if (t instanceof Exception) {
                SysLog.log(Log.ERROR, t, "Exception caught in main...");
            } else {
                SysLog.log(Log.ERROR, t, "Throwable caught in main...");
            }
        }
    }

    /**
     * Loads and returns the bytes of the given model file.
     *
     * @param fileName
     * @return
     */
    private static byte[] loadModelBytes(String fileName) {

        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(fileName);

            // Allocate input buffer
            FileChannel channel = inStream.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());

            // Read the bytes
            channel.read(buffer);
            return buffer.array();
        } catch (Throwable t) {
            System.out.println("Failed to load model file: " + fileName);
            t.printStackTrace(System.out);
            System.exit(3);
            return null;    // Will never get here, but makes compiler happy
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    /**
     * This method waits for the given job to complete within the given
     * number of seconds.  The final status of the job is returned in the
     * JobInfoValue object.
     *
     * @param jobId
     * @param maxWaitSeconds
     * @return JobInfoValue
     * @throws InterruptedException
     * @throws PSEException
     */
    private static JobInfoValue waitForJob(String jobId, int maxWaitSeconds)
            throws PSEException, InterruptedException {

        JobInfoValue jobDetails = null;
        for (int i = 0; i < maxWaitSeconds; i++) {    // Limit wait time

            Thread.sleep(1000);                       // Pause 1 second

            // Get updated job details.  Note this is much faster than
            // pse.getJobStatus() because it does not have detailed
            // progress information (workitems started, workitems completed, etc).

            jobDetails = SysPSE.getPSE().getJob(jobId, false);
            if (jobDetails.getStatus() == PSEUtils.JOB_DONE) {
                break;
            }
        }

        return jobDetails;    // Return current job details to caller
    }

    private static void printJobResult(DtComponent comp, String jobid) {
        try {
            if (comp.getParameterList().size() > 0) {
                ResultRequestMgr resultrequestmgr = ResultRequestMgrFactory.createInstanceWithAllVariables(jobid, new DtComponentPathDescriptor(new DtComponentPath(comp).getPath()));


            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static ArrayList removeNoSaveDBParametersFromVariableReferenceList(List list)
            throws VariableException {
        HashSet hashset = new HashSet(list);
        Iterator iterator = hashset.iterator();
        do {
            if (!iterator.hasNext())
                break;
            VariableReference variablereference = (VariableReference) iterator.next();
            if (!variablereference.getVariable().isSaveToDB())
                iterator.remove();
        } while (true);
        return new ArrayList(hashset);
    }
}

