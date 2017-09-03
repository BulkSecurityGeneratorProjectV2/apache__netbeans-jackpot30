/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.java.hints.infrastructure;

import com.sun.source.util.TreePath;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.api.lexer.Language;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.java.source.TreeLoader;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.LifecycleManager;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

/**
 * @author Jan Lahoda
 */
public abstract class TreeRuleTestBase extends NbTestCase {
    protected final Logger LOG;
    
    public TreeRuleTestBase(String name) {
        super(name);
        LOG = Logger.getLogger("test." + name);
    }
    
    @Override
    protected Level logLevel() {
        return Level.INFO;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SourceUtilsTestUtil.prepareTest(new String[] {"org/netbeans/modules/java/editor/resources/layer.xml"}, new Object[0]);
        TreeLoader.DISABLE_CONFINEMENT_TEST = true;
    }

    private void prepareTest(String fileName, String code) throws Exception {
        clearWorkDir();
        File wdFile = getWorkDir();
        FileUtil.refreshFor(wdFile);

        FileObject wd = FileUtil.toFileObject(wdFile);
        assertNotNull(wd);
        sourceRoot = FileUtil.createFolder(wd, "src");
        FileObject buildRoot = FileUtil.createFolder(wd, "build");
        FileObject cache = FileUtil.createFolder(wd, "cache");

        FileObject data = FileUtil.createData(sourceRoot, fileName);
        File dataFile = FileUtil.toFile(data);
        
        assertNotNull(dataFile);
        
        TestUtilities.copyStringToFile(dataFile, code);
        
        SourceUtilsTestUtil.prepareTest(sourceRoot, buildRoot, cache, extraClassPath());
        
        DataObject od = DataObject.find(data);
        EditorCookie ec = od.getCookie(EditorCookie.class);
        
        assertNotNull(ec);
        
        doc = ec.openDocument();
        doc.putProperty(Language.class, JavaTokenId.language());
        doc.putProperty("mimeType", "text/x-java");
        
        JavaSource js = JavaSource.forFileObject(data);
        
        assertNotNull(js);
        
        info = SourceUtilsTestUtil.getCompilationInfo(js, Phase.RESOLVED);
        
        assertNotNull(info);
    }
    
    private FileObject sourceRoot;
    private CompilationInfo info;
    private Document doc;
    
    protected abstract List<ErrorDescription> computeErrors(CompilationInfo info, TreePath path);
    
    protected List<ErrorDescription> computeErrors(CompilationInfo info, TreePath path, int offset) {
        return computeErrors(info, path);
    }
    
    protected String toDebugString(CompilationInfo info, Fix f) {
        return f.toString();
    }
    
    protected void performAnalysisTest(String fileName, String code, String... golden) throws Exception {
        int[] offset = new int[1];
        
        code = org.netbeans.modules.java.hints.TestUtilities.detectOffsets(code, offset);
        
        performAnalysisTest(fileName, code, offset[0], golden);
    }
    
    protected void performAnalysisTest(String fileName, String code, int pos, String... golden) throws Exception {
        prepareTest(fileName, code);
        
        TreePath path = info.getTreeUtilities().pathFor(pos);
        
        List<ErrorDescription> errors = computeErrors(info, path, pos);
        List<String> errorsNames = new LinkedList<String>();
        
        errors = errors != null ? errors : Collections.<ErrorDescription>emptyList();
        
        for (ErrorDescription e : errors) {
            errorsNames.add(e.toString());
        }
        
        assertTrue("The warnings provided by the hint do not match expected warnings. Provided warnings: " + errorsNames.toString(), Arrays.equals(golden, errorsNames.toArray(new String[0])));
    }
    
    protected String performFixTest(String fileName, String code, String errorDescriptionToString, String fixDebugString, String golden) throws Exception {
        int[] offset = new int[1];

        code = org.netbeans.modules.java.hints.TestUtilities.detectOffsets(code, offset);
        
        return performFixTest(fileName, code, offset[0], errorDescriptionToString, fixDebugString, golden);
    }
    
    protected String performFixTest(String fileName, String code, int pos, String errorDescriptionToString, String fixDebugString, String golden) throws Exception {
        return performFixTest(fileName, code, pos, errorDescriptionToString, fixDebugString, fileName, golden);
    }
    
    protected String performFixTest(String fileName, String code, String errorDescriptionToString, String fixDebugString, String goldenFileName, String golden) throws Exception {
        int[] offset = new int[1];

        code = org.netbeans.modules.java.hints.TestUtilities.detectOffsets(code, offset);

        return performFixTest(fileName, code, offset[0], errorDescriptionToString, fixDebugString, goldenFileName, golden);
    }
    
    protected String performFixTest(String fileName, String code, int pos, String errorDescriptionToString, String fixDebugString, String goldenFileName, String golden) throws Exception {
        prepareTest(fileName, code);
        
        TreePath path = info.getTreeUtilities().pathFor(pos);
        
        List<ErrorDescription> errors = computeErrors(info, path, pos);
        
        ErrorDescription toFix = null;
        
        for (ErrorDescription d : errors) {
            if (errorDescriptionToString.equals(d.toString())) {
                toFix = d;
                break;
            }
        }
        
        assertNotNull("Error: \"" + errorDescriptionToString + "\" not found. All ErrorDescriptions: " + errors.toString(), toFix);
        
        assertTrue("Must be computed", toFix.getFixes().isComputed());
        
        List<Fix> fixes = toFix.getFixes().getFixes();
        List<String> fixNames = new LinkedList<String>();
        Fix toApply = null;
        
        for (Fix f : fixes) {
            if (fixDebugString.equals(toDebugString(info, f))) {
                toApply = f;
            }
            
            fixNames.add(toDebugString(info, f));
        }
        
        assertNotNull("Cannot find fix to invoke: " + fixNames.toString(), toApply);
        
        toApply.implement();
        
        FileObject toCheck = sourceRoot.getFileObject(goldenFileName);
        
        assertNotNull(toCheck);
        
        DataObject toCheckDO = DataObject.find(toCheck);
        EditorCookie ec = toCheckDO.getLookup().lookup(EditorCookie.class);
        Document toCheckDocument = ec.openDocument();
        
        String realCode = toCheckDocument.getText(0, toCheckDocument.getLength());
        
        //ignore whitespaces:
        realCode = realCode.replaceAll("[ \t\n]+", " ");

        if (golden != null) {
            assertEquals("The output code does not match the expected code.", golden, realCode);
        }
        
        LifecycleManager.getDefault().saveAll();

        return realCode;
    }
    
    protected FileObject[] extraClassPath() {
        return new FileObject[0];
    }

    // common tests to check nothing is reported
    public void testIssue105979() throws Exception {
        String before = "package test; class Test {" +
                "  return b;" +
                "}\n";

        for (int i = 0; i < before.length(); i++) {
            LOG.info("testing position " + i + " at " + before.charAt(i));
            clearWorkDir();
            performAnalysisTest("test/Test.java", before, i);
}
    }
    public void testIssue108246() throws Exception {

        String before = "package test; class Test {" +
            "  Integer ii = new Integer(0);" +
            "  String s = ii.toString();" +
            "\n}\n";

        for (int i = 0; i < before.length(); i++) {
            LOG.info("testing position " + i + " at " + before.charAt(i));
            clearWorkDir();
            performAnalysisTest("test/Test.java", before, i);
        }
    }

    public void testNoHintsForSimpleInitialize() throws Exception {

        String before = "package test; class Test {" +
            " { java.lang.System.out.println(); } " +
            "}\n";

        for (int i = 0; i < before.length(); i++) {
            LOG.info("testing position " + i + " at " + before.charAt(i));
            clearWorkDir();
            performAnalysisTest("test/Test.java", before, i);
        }
    }

    public void testIssue113933() throws Exception {

        String before = "package test; class Test {" +
            "  public void test() {" +
            "  super.A();" +
            "\n}\n}\n";

        for (int i = 0; i < before.length(); i++) {
            LOG.info("testing position " + i + " at " + before.charAt(i));
            clearWorkDir();
            performAnalysisTest("test/Test.java", before, i);
        }
    }
}
