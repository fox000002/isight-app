package org.huys.isight;

import com.engineous.common.SDKUtils;
import com.engineous.common.i18n.IString;
import com.engineous.common.io.IOUtil;
import com.engineous.sdk.exception.SDKException;
import com.engineous.sdk.frapi.*;
import com.engineous.sdk.frapi.exception.EmptyResultException;
import com.engineous.sdk.frapi.exception.ResultException;
import com.engineous.sdk.gui.ProgressRange;
import com.engineous.sdk.log.Log;
import com.engineous.sdk.log.SysLog;
import com.engineous.sdk.model.*;
import com.engineous.sdk.model.exceptions.DtModelException;
import com.engineous.sdk.model.path.DtComponentPathDescriptor;
import com.engineous.sdk.pse.*;
import com.engineous.sdk.resmgr.ResMgr;
import com.engineous.sdk.runtime.Context;
import com.engineous.sdk.runtime.RunResultData;
import com.engineous.sdk.vars.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

// Referenced classes of package com.engineous.sdk.runtime:
//            RunResultData, RtException, Context

public class RtUtils
{
    private static class JobLogValueComparator
        implements Comparator
    {

        public int compare(Object obj, Object obj1)
        {
            JobLogValue joblogvalue = (JobLogValue)obj;
            JobLogValue joblogvalue1 = (JobLogValue)obj1;
            return joblogvalue.getLoggingTime().compareTo(joblogvalue1.getLoggingTime());
        }

        JobLogValueComparator()
        {
        }
    }


    public RtUtils()
    {
    }

    public static void mapContext(Context context, Context context1)
        throws VariableException
    {
        mapContext(context, context1, ((List) (new ArrayList())));
    }

    public static void mapContext(Context context, Context context1, List list)
        throws VariableException
    {
        if(context1 == null || context == null)
            return;
        Iterator iterator = null;
        iterator = context1.getList().iterator();
        do
        {
            if(!iterator.hasNext())
                break;
            Variable variable = (Variable)iterator.next();
            if(list == null || !list.contains(variable.getName()))
            {
                int i = variable.getMode();
                String s = variable.getID();
                Variable variable1 = context.getVariableById(s);
                if(i == 3 || i == 2)
                {
                    if(variable.getStructure() == 2)
                        ((ArrayVariable)variable1).setDimSize(((ArrayVariable)variable).getDimSize());
                    context.getVariableById(s).mapFrom(variable);
                }
            }
        } while(true);
    }

    public static void mapContext(Context context, Context context1, VariableFilter variablefilter)
        throws VariableException
    {
        if(context1 == null || context == null)
            return;
        Iterator iterator = null;
        iterator = context1.getList().iterator();
        do
        {
            if(!iterator.hasNext())
                break;
            Variable variable = (Variable)iterator.next();
            int i = variable.getMode();
            String s = variable.getID();
            Variable variable1 = context.getVariableById(s);
            if((i == 3 || i == 2) && variablefilter.accept(variable))
            {
                if(variable.getStructure() == 2)
                    ((ArrayVariable)variable1).setDimSize(((ArrayVariable)variable).getDimSize());
                variable1.mapFrom(variable);
            }
        } while(true);
    }

    public static void writeResults(DtComponentPath dtcomponentpath, String s, File file, String s1)
        throws SDKException
    {
        writeResults(dtcomponentpath, s, file, true, null, s1);
    }

    public static void writeResults(DtComponentPath dtcomponentpath, String s, File file, boolean flag, String s1)
        throws SDKException
    {
        writeResults(dtcomponentpath, s, file, flag, null, s1);
    }

    public static void writeResults(DtComponentPath dtcomponentpath, String s, File file, boolean flag, ProgressRange progressrange, String s1)
        throws SDKException
    {
        DtComponent dtcomponent = dtcomponentpath.getLastPathComponent();
        try
        {
            if(dtcomponent.getParameterList().size() > 0)
            {
                DtComponent dtcomponent1 = dtcomponentpath.getLastPathComponent();
                ResultRequestMgr resultrequestmgr = ResultRequestMgrFactory.createInstanceWithAllVariables(s, new DtComponentPathDescriptor(dtcomponentpath.getPath()));
                if(progressrange != null)
                {
                    progressrange.setIntervals(100);
                    //progressrange.setProgress(ResMgr.getMessage(CLASS, 0x1641e, "Writing results to file {0}", file.getAbsolutePath()), 50);
                }
                writeResults(dtcomponentpath, s, file, null, flag, progressrange, s1);
            }
            if(dtcomponent.isInstanceOf("com.engineous.system.component.Process"))
            {
                DtComponentPath dtcomponentpath2;
                for(Iterator iterator = dtcomponent.getComponentIterator(); iterator.hasNext(); writeResults(dtcomponentpath2, s, file, flag, progressrange, s1))
                {
                    DtComponent dtcomponent3 = (DtComponent)iterator.next();
                    dtcomponentpath2 = new DtComponentPath(dtcomponentpath.getPath(), dtcomponent3);
                }

            } else
            if(dtcomponent.isReference())
            {
                DtComponent dtcomponent2 = null;
                try
                {
                    dtcomponent2 = dtcomponent.getReferenceRoot();
                }
                catch(Throwable throwable) { }
                if(dtcomponent2 != null)
                {
                    DtComponentPath dtcomponentpath1 = new DtComponentPath(dtcomponentpath.getPath(), dtcomponent2);
                    writeResults(dtcomponentpath1, s, file, flag, progressrange, s1);
                }
            }
            //if(progressrange != null)
                //progressrange.setProgress(ResMgr.getMessage(CLASS, 46311, "Finished writing results to file {0}", file.getAbsolutePath()), 100);
        }
        catch(Exception exception)
        {
            //throw new SDKException(exception, new IString(CLASS, 43899, "Failure writing results to disk"));
        }
    }

    private static ArrayList removeNoSaveDBParametersFromVariableReferenceList(List list)
        throws VariableException
    {
        HashSet hashset = new HashSet(list);
        Iterator iterator = hashset.iterator();
        do
        {
            if(!iterator.hasNext())
                break;
            VariableReference variablereference = (VariableReference)iterator.next();
            if(!variablereference.getVariable().isSaveToDB())
                iterator.remove();
        } while(true);
        return new ArrayList(hashset);
    }

    public static void writeResults(DtComponentPath dtcomponentpath, String s, File file, File file1, boolean flag, ProgressRange progressrange, String s1)
        throws SDKException
    {
        try
        {
            DtComponent dtcomponent = dtcomponentpath.getLastPathComponent();
            if(dtcomponent.getParameterList().size() > 0)
            {
                if(progressrange != null)
                {
                    progressrange.setIntervals(100);
                    //progressrange.setProgress(ResMgr.getMessage(CLASS, 0x110a9, "Writing results to {0}", file.getAbsolutePath()), 50);
                }
                if(dtcomponent.isInstanceOf("com.engineous.system.component.Process"))
                {
                    Object obj = VariableUtil.getSubflowVariableReferenceList(dtcomponent);
                    obj = removeNoSaveDBParametersFromVariableReferenceList(((List) (obj)));
                    ResultRequestMgr resultrequestmgr = ResultRequestMgrFactory.createInstanceWithVarRefs(s, new DtComponentPathDescriptor(dtcomponentpath.getPath()), ((List) (obj)), true);
                    boolean flag1 = resultrequestmgr.isComplete();
                    String s2 = dtcomponentpath.getPathAsString();
                    String s4 = s2;
                    try
                    {
                        resultrequestmgr.getSynchronousResults();
                    }
                    catch(EmptyResultException emptyresultexception) { }
                    FiperResultSet fiperresultset;
                    for(fiperresultset = resultrequestmgr.getResultSetSnapShot(); !resultrequestmgr.isComplete() && fiperresultset.next(););
                    File file2 = file1;
                    if(file2 != null)
                    {
                        String s6 = file2.getAbsolutePath();
                        String s7 = IOUtil.extractFileExtensionPart(s6);
                        String s8 = IOUtil.extractFileNamePart(s6);
                        if(s7 != null)
                            s7 = (new StringBuilder()).append(".").append(s7).toString();
                        else
                            s7 = "";
                        file2 = new File(IOUtil.extractFileDirPart(s6), (new StringBuilder()).append(s8).append("-subflow").append(s7).toString());
                    }
                    writeResultsToDisk(dtcomponentpath, fiperresultset, file, file2, flag, progressrange, s1, true);
                }
                Object obj1 = VariableUtil.flattenVariableCollectionToVariableReferenceList(dtcomponent.getParameterList(), false, true);
                obj1 = removeNoSaveDBParametersFromVariableReferenceList(((List) (obj1)));
                ResultRequestMgr resultrequestmgr1 = ResultRequestMgrFactory.createInstanceWithVarRefs(s, new DtComponentPathDescriptor(dtcomponentpath.getPath()), ((List) (obj1)), false);
                boolean flag2 = resultrequestmgr1.isComplete();
                String s3 = dtcomponentpath.getPathAsString();
                String s5 = s3;
                try
                {
                    resultrequestmgr1.getSynchronousResults();
                }
                catch(EmptyResultException emptyresultexception1) { }
                FiperResultSet fiperresultset1;
                for(fiperresultset1 = resultrequestmgr1.getResultSetSnapShot(); !resultrequestmgr1.isComplete() && fiperresultset1.next(););
                writeResultsToDisk(dtcomponentpath, fiperresultset1, file, file1, flag, progressrange, s1);
               // if(progressrange != null)
                    //progressrange.setProgress(ResMgr.getMessage(CLASS, 17571, "Finished writing results to {0}", file.getAbsolutePath()), 100);
            }
        }
        catch(Throwable throwable)
        {
            throw new SDKException(throwable);
        }
    }

    private static void writeResultsToDisk(DtComponentPath dtcomponentpath, FiperResultSet fiperresultset, File file, File file1, boolean flag, ProgressRange progressrange, String s)
        throws SDKException
    {
        writeResultsToDisk(dtcomponentpath, fiperresultset, file, file1, flag, progressrange, s, false);
    }

    private static void writeResultsToDisk(DtComponentPath dtcomponentpath, FiperResultSet fiperresultset, File file, File file1, boolean flag, ProgressRange progressrange, String s, boolean flag1)
        throws SDKException
    {
        try
        {
            if(file != null && !file.exists())
            {
                boolean flag2 = IOUtil.mkdirs(file);
                //if(!flag2)
                   // throw new SDKException(new IString(CLASS, 55526, "Failed to create directory {0}", file.getAbsolutePath()));
            }
            boolean flag3 = false;
            Object obj = null;
            int i = 0;
            int j = 0;
            String s1 = null;
            try
            {
                s1 = dtcomponentpath.getPathAsString();
            }
            catch(Exception exception)
            {
                throw new SDKException(exception);
            }
            if(file1 == null)
            {
                char ac[] = {
                    '\\', '/', ':', '*', '?', '<', '>', '|'
                };
                for(int k = 0; k < ac.length; k++)
                    s1 = s1.replace(ac[k], '_');

                if(flag1)
                    s1 = (new StringBuilder()).append(s1).append("-subflow").toString();
                if(s.indexOf(",") > -1)
                    file1 = new File(file, (new StringBuilder()).append(s1).append(".csv").toString());
                else
                    file1 = new File(file, (new StringBuilder()).append(s1).append(".txt").toString());
            }
            RunResultData runresultdata = new RunResultData(dtcomponentpath.getLastPathComponent(), fiperresultset);
            if(progressrange != null)
                progressrange.beginSubrange(j * i + 10, (j + 1) * i + 10);
            ArrayList arraylist = new ArrayList();
            File file2 = null;
            //if(progressrange != null)
            //    progressrange.setProgress(ResMgr.getMessage(CLASS, 51089, "Writing results for {0}", s1));
            if(flag)
                try
                {
                    Collection collection = null;
                    if(flag1)
                        collection = VariableUtil.filterType(dtcomponentpath.getLastPathComponent().getSubFlowParameterList().getList(), "com.engineous.datatype.File", true);
                    else
                        collection = VariableUtil.filterType(dtcomponentpath.getLastPathComponent().getParameterList(), "com.engineous.datatype.File", true);
                    if(collection.size() > 0)
                    {
                        file2 = new File(file, (new StringBuilder()).append(s1).append("-Files").toString());
                        IOUtil.mkdirs(file2);
                    }
                    int l = runresultdata.getNumRows();
                    Iterator iterator = collection.iterator();
                    do
                    {
                        if(!iterator.hasNext())
                            break;
                        Variable variable = (Variable)iterator.next();
                        if(variable.getStructure() == 1)
                        {
                            ScalarVariable scalarvariable = (ScalarVariable)variable;
                            int i1 = 0;
                            while(i1 < l) 
                            {
                                String s2;
                                if(l > 1)
                                    s2 = (new StringBuilder()).append("-").append(Integer.toString(i1 + 1)).toString();
                                else
                                    s2 = "";
                                FileValueType filevaluetype = (FileValueType)runresultdata.getValue(i1, variable);
                                if(filevaluetype != null)
                                {
                                    String s3 = filevaluetype.getHandler().getOriginalFileName();
                                    if(s3 == null)
                                        s3 = filevaluetype.getRawFileName();
                                    if(s3 != null)
                                    {
                                        s3 = IOUtil.scrubFileName(s3);
                                        String s4 = IOUtil.extractFileExtensionPart(s3);
                                        if(s4 != null)
                                            s2 = (new StringBuilder()).append(s2).append(".").append(s4).toString();
                                    }
                                    writeFileValue(file2, filevaluetype, scalarvariable.getName(), s2);
                                } else
                                {
                                    //SysLog.getLog().logWarn(new IString(CLASS, 0x13f4a, "File value is not available. Is the file parameter set to save to the database?"));
                                }
                                i1++;
                            }
                        }
                    } while(true);
                }
                catch(Exception exception1)
                {
                    flag3 = true;
                    obj = exception1;
                }
            if(file2 != null && file2.list().length == 0)
                file2.delete();
            SDKUtils.writeRunData(file1.getAbsolutePath(), fiperresultset, s);
            if(progressrange != null)
                progressrange.endSubrange();
            //if(flag3)
            //    throw new SDKException(((Throwable) (obj)), new IString(CLASS, 35863, "Failure writing file parameters to disk"));
        }
        catch(SDKException sdkexception)
        {
            throw sdkexception;
        }
        catch(ResultException resultexception)
        {
            throw new SDKException(resultexception);
        }
    }

    public static File writeFileValue(File file, FileValueType filevaluetype, String s, String s1)
        throws IOException, VariableException
    {
        String s2 = s;
        int i = s2.lastIndexOf('_');
        if(s2.indexOf('.') < 0 && i > 0 && (s1 == null || s2.substring(i + 1).equals(s1.substring(Math.max(0, s1.length() - i)))))
        {
            StringBuffer stringbuffer = new StringBuffer(s2);
            stringbuffer.setCharAt(i, '.');
            s2 = stringbuffer.toString();
        }
        if(s1 != null && !s2.endsWith(s1))
        {
            int j = s2.lastIndexOf(".");
            if(j > 0)
            {
                StringBuffer stringbuffer1 = new StringBuffer(s2);
                if(s1.endsWith(s2.substring(j)))
                    stringbuffer1.replace(j, s2.length(), s1);
                else
                    stringbuffer1.insert(j, s1);
                s2 = stringbuffer1.toString();
            } else
            {
                s2 = (new StringBuilder()).append(s2).append(s1).toString();
            }
        }
        File file1 = new File(s2.replace('\\', '/'));
        File file2 = new File(file, file1.getName());
        if(file2.exists())
        {
            String s3 = file1.getName();
            int k = s3.lastIndexOf('.');
            String s4;
            if(k > 0)
            {
                s4 = s3.substring(k);
                s3 = s3.substring(0, k);
            } else
            {
                s4 = "";
            }
            int l = 0;
            do
            {
                l++;
                file2 = new File(file, (new StringBuilder()).append(s3).append(l).append(s4).toString());
            } while(file2.exists());
        }
        FileOutputStream fileoutputstream = new FileOutputStream(file2);
        InputStream inputstream = null;
        //String s5 = ResMgr.getMessage(CLASS, 23768, "File could not be accessed");
        try
        {
            inputstream = filevaluetype.getHandler().getInputStream();
        }
        catch(Exception exception)
        {
            //s5 = (new StringBuilder()).append(s5).append("\n").append(exception.toString()).toString();
        }
        if(inputstream == null)
        {
            //fileoutputstream.write(s5.getBytes());
            fileoutputstream.close();
        } else
        if(filevaluetype.getDataType().equals("text/plain"))
        {
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream, filevaluetype.getDataEncoding());
            OutputStreamWriter outputstreamwriter = new OutputStreamWriter(fileoutputstream, IOUtil.getLocalEncoding());
            IOUtil.copyChars(inputstreamreader, outputstreamwriter);
        } else
        {
            IOUtil.copyStream(inputstream, fileoutputstream);
        }
        return file2;
    }

//    public static void writeJobLogMessages(String s, File file)
//        throws SDKException
//    {
//        FileOutputStream fileoutputstream;
//        OutputStreamWriter outputstreamwriter;
//        if(file.exists())
//            if(file.isDirectory())
//                file = new File(file, IOUtil.scrubFileName((new StringBuilder()).append("JobLog-").append(s).toString()));
//            else
//            if(!file.canWrite())
//                throw new SDKException(new IString(CLASS, 0x13f6d, "Job log file {0} cannot be overwritten.", file));
//        File file1 = file.getParentFile();
//        if(file1 != null && !file1.exists())
//        {
//            boolean flag = IOUtil.mkdirs(file1);
//            if(!flag)
//                throw new SDKException(new IString(CLASS, 55526, "Failed to create directory {0}", file1.getAbsolutePath()));
//        }
//        fileoutputstream = null;
//        outputstreamwriter = null;
//        try
//        {
//            fileoutputstream = new FileOutputStream(file);
//            outputstreamwriter = new OutputStreamWriter(fileoutputstream, IOUtil.getLocalEncoding());
//            Collection collection = SysPSE.getPSE().getLogsForJob(s);
//            if(collection == null || collection.size() < 1)
//            {
//                outputstreamwriter.write(ResMgr.getMessage(CLASS, 0x13309, "Job {0} has no log.\n", s));
//            } else
//            {
//                outputstreamwriter.write(ResMgr.getMessage(CLASS, 9629, "Job {0} logs:\n", s));
//                Object obj;
//                if(collection instanceof List)
//                    obj = (List)collection;
//                else
//                    obj = new ArrayList(collection);
//                Collections.sort(((List) (obj)), new JobLogValueComparator());
//                SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
//                Iterator iterator = ((List) (obj)).iterator();
//                do
//                {
//                    if(!iterator.hasNext())
//                        break;
//                    JobLogValue joblogvalue = (JobLogValue)iterator.next();
//                    outputstreamwriter.write(ResMgr.getMessage(CLASS, 48604, "\nMessage: {0}\nLogged Time: {1} Severity: {2,number} Source: {3}\n", new Object[] {
//                        joblogvalue.getMsg(), simpledateformat.format(joblogvalue.getLoggingTime()), new Integer(joblogvalue.getSeverity()), joblogvalue.getSource()
//                    }));
//                    Throwable throwable = joblogvalue.getException();
//                    if(throwable != null)
//                        outputstreamwriter.write(ResMgr.getMessage(CLASS, 0x175aa, "Exception: {0}\n", throwable));
//                } while(true);
//                outputstreamwriter.flush();
//            }
//        }
//        catch(PSEException pseexception)
//        {
//            throw new SDKException(pseexception, new IString(CLASS, 0x13e44, "Failed to fetch logs for job {0}.", s));
//        }
//        catch(Exception exception)
//        {
//            throw new SDKException(exception, new IString(CLASS, 20091, "Unable to write logs for job {0}.", s));
//        }
//        if(outputstreamwriter != null)
//            try
//            {
//                outputstreamwriter.close();
//            }
//            catch(IOException ioexception) { }
//        else
//        if(fileoutputstream != null)
//            try
//            {
//                fileoutputstream.close();
//            }
//            catch(IOException ioexception1) { }
////        break MISSING_BLOCK_LABEL_527;
//        Exception exception1;
////        exception1;
//        if(outputstreamwriter != null)
//            try
//            {
//                outputstreamwriter.close();
//            }
//            catch(IOException ioexception2) { }
//        else
//        if(fileoutputstream != null)
//            try
//            {
//                fileoutputstream.close();
//            }
//            catch(IOException ioexception3) { }
//        throw exception1;
//    }
//
//    public static String generateJobDesc(DtComponent dtcomponent)
//        throws RtException
//    {
//        String s = null;
//        if(dtcomponent == null)
//            throw new RtException(new IString(CLASS, 1637, "Root component is null for job description generation"));
//        try
//        {
//            DtModelManager dtmodelmanager = dtcomponent.getModelManager();
//            if(dtmodelmanager != null)
//            {
//                ModelProperties modelproperties = dtmodelmanager.getModelProperties();
//                if(modelproperties != null)
//                    s = modelproperties.getModelName();
//            }
//            if(s == null || s.length() <= 0)
//                s = dtcomponent.getName();
//            SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
//            Date date = new Date();
//            s = (new StringBuilder()).append(s).append(" - ").append(simpledateformat.format(date)).toString();
//        }
//        catch(DtModelException dtmodelexception)
//        {
//            throw new RtException(dtmodelexception);
//        }
//        return s;
//    }

//    public static String getEvalTypeDisplayName(int i)
//    {
//        switch(i)
//        {
//        case 1: // '\001'
//            return ResMgr.getMessage(CLASS, 0x16be8, "Normal");
//
//        case 0: // '\0'
//            return ResMgr.getMessage(CLASS, 0x1415e, "Exact");
//
//        case 2: // '\002'
//            return ResMgr.getMessage(CLASS, 0x13455, "Approximation Update");
//
//        case 3: // '\003'
//            return ResMgr.getMessage(CLASS, 32546, "DB Rerun Lookup");
//
//        case 5: // '\005'
//            return ResMgr.getMessage(CLASS, 28568, "DB Lookup");
//
//        case 4: // '\004'
//        default:
//            return ResMgr.getMessage(CLASS, 0x11a0c, "(Unrecognized Evaluation Type)");
//        }
//    }
//
//    private static final transient Class CLASS = com/engineous/sdk/runtime/RtUtils;

}
