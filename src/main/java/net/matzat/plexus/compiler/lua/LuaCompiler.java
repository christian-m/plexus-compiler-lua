package net.matzat.plexus.compiler.lua;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.util.IOUtil;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.DumpState;
import org.luaj.vm2.compiler.LuaC;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @plexus.component role="org.codehaus.plexus.compiler.Compiler"
 * role-hint="lua"
 */
public class LuaCompiler extends AbstractCompiler {

    private final static String LUA_SUFFIX = ".lua";

    public LuaCompiler() {
        super(CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, LUA_SUFFIX, LUA_SUFFIX, null);
    }

    protected LuaCompiler(CompilerOutputStyle compilerOutputStyle, String inputFileEnding, String outputFileEnding, String outputFile) {
        super(CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, LUA_SUFFIX, LUA_SUFFIX, outputFile);
    }

    @Override
    public CompilerResult performCompile(CompilerConfiguration compilerConfiguration) throws CompilerException {
        File destinationDir = new File(compilerConfiguration.getOutputLocation());
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }
        Set<File> sourceFiles = compilerConfiguration.getSourceFiles();

        if (sourceFiles.size() == 0) {
            return new CompilerResult().success(true);
        }

        List<CompilerMessage> messages = new ArrayList<>();
        messages.add(new CompilerMessage("Compiling " + sourceFiles.size() + " " + "source file" +
                (sourceFiles.size() == 1 ? "" : "s") + " to " + destinationDir.getAbsolutePath(), CompilerMessage.Kind.NOTE));

        // LuaJ 3.0.1
        // final Globals globals = JsePlatform.standardGlobals();

        for (File sourceFile : sourceFiles) {
            final String sourceFileName = sourceFile.getName();
            final String chunkName = sourceFileName.substring(0, sourceFileName.length() - 4);
            InputStream sourceFileInputStream = null;
            OutputStream compiledFileOutputStream = null;
            try {
                sourceFileInputStream = new BufferedInputStream(new FileInputStream(sourceFile));
                // TODO Handle folder structure on destination files
                final String destinationFile = destinationDir.getAbsolutePath() + File.separatorChar + chunkName + LUA_SUFFIX;
                messages.add(new CompilerMessage("Compiling File " + destinationFile, CompilerMessage.Kind.NOTE));
                compiledFileOutputStream = new FileOutputStream(destinationFile);
                // LuaJ 3.0.1 Compiler: Prototype chunk = globals.compilePrototype(sourceFileInputStream, chunkName);
                Prototype chunk = LuaC.instance.compile(sourceFileInputStream, chunkName);
                DumpState.dump(chunk, compiledFileOutputStream, false, DumpState.NUMBER_FORMAT_DEFAULT, false);
            } catch (IOException e) {
                messages.add(new CompilerMessage("Failed compiling file " + chunkName, CompilerMessage.Kind.WARNING));
            } finally {
                IOUtil.close(sourceFileInputStream);
                IOUtil.close(compiledFileOutputStream);
            }
        }


        return new CompilerResult().compilerMessages(messages);
    }

    public boolean canUpdateTarget(CompilerConfiguration configuration)
            throws CompilerException {
        return false;
    }

    public String[] createCommandLine(CompilerConfiguration compilerConfiguration) throws CompilerException {
        return new String[0];
    }

}
