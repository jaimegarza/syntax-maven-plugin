/*
 ===============================================================================
 Copyright (c) 1985, 2012, 2016 Jaime Garza
 All rights reserved.
 
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the
       names of its contributors may be used to endorse or promote products
       derived from this software without specific prior written permission.
 
 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ===============================================================================
 */
package me.jaimegarza.syntax.plugin;

import java.io.File;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import me.jaimegarza.syntax.Syntax;
import me.jaimegarza.syntax.env.Environment;
import me.jaimegarza.syntax.exception.AnalysisException;
import me.jaimegarza.syntax.exception.OutputException;
import me.jaimegarza.syntax.exception.ParsingException;
import me.jaimegarza.syntax.language.Language;
import me.jaimegarza.syntax.language.LanguageSupport;
import me.jaimegarza.syntax.util.PathUtils;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

@Mojo(name="generate")
public class SyntaxMavenPluginMojo extends AbstractMojo {
  private static final int ARGS = 10;

  /**
   * Name of the source file.
   */
  @Parameter(name="sourceFile", required=true)
  private File sourceFile;

  /**
   * Name of the output file to generate.
   */
  @Parameter(name="outputFile", required=true)
  private File outputFile;

  /**
   * Name of the include file to generate.
   */
  @Parameter(name="includeFile")
  private File includeFile;
  
  /**
   * Name of the textual report file with summaries and additional detail.
   */
  @Parameter(name="reportFile")
  private File reportFile;

  /**
   * Uses the external skeleton provided, default is internal skeleton.
   */
  @Parameter(name="skeletonFile")
  private File skeletonFile;

  /**
   * Produce a resource bundle for the error messages. No bundle if none specified. Java.
   */
  @Parameter(name="bundleFile")
  private File bundleFile;

  /**
   * Setup the syntax and output to be either java|c|pascal, default java.
   */
  @Parameter(name="language", defaultValue="java")
  private String language;

  /**
   * The algorithm to be used, either slr or lalr, default is lalr.
   */
  @Parameter(name="algorithm", defaultValue="lalr")
  private String algorithm;

  /**
   * Generate include file, default is false)
   */
  @Parameter(name="externalInclude", defaultValue="false")
  private boolean externalInclude;

  /**
   * Produce verbose output
   */
  @Parameter(name="verbose", defaultValue="false")
  private boolean verbose;

  /**
   * Produce debugging output
   */
  @Parameter(name="debug", defaultValue="false")
  private boolean debug;

  /**
   * Emit lines in C if true, default true
   */
  @Parameter(name="emitLine", defaultValue="true")
  private boolean emitLine;

  /**
   * Packing format of parser (packed|tabular, default packed)
   *   please note that unpacked tables are mostly for didactical 
   *   purposes as they  may lend a big number of states in a
   *   sparsely populated table.
   */
  @Parameter(name="packed", defaultValue="true")
  private boolean packed;
  
  /**
   * Run only the tokenizer, dumping the tokens in the process.
   */
  @Parameter(name="tokenizer", defaultValue="false")
  private boolean tokenizer;

  /**
   * What parser driver is to be used (parser|scanner, default is parser)
   */
  @Parameter(name="driver", defaultValue="parser")
  private String driver;
  
  /**
   * Right margin on generated source
   */
  @Parameter(name="margin", defaultValue="-1")
  private int margin;

  /**
   * Indent by n spaces, default 2
   */
  @Parameter(name="indent", defaultValue="-1")
  private int indent;

  private int numberOfArgs;
  private int numberOfFlags;
  private int numberOfFiles;

  @Override
  public void execute() throws MojoExecutionException {

    try {
      countArgs();
      countFlags();
      countFiles();
  
      int i;
      String args[] = new String[numberOfArgs + numberOfFlags + numberOfFiles];
      
      i = setArguments(args);
      i = setFlags(i, args);
      setFiles(i, args);
      
      Environment environment = new Environment("Syntax", args);
      Syntax syntax = new Syntax(environment);
      try {
        syntax.executeInternal();
      } catch (ParsingException e) {
        throw new MojoExecutionException("the source file cannot be parsed", e);
      } catch (AnalysisException e) {
        throw new MojoExecutionException("the source file cannot be analyzed", e);
      } catch (OutputException e) {
        throw new MojoExecutionException("the source file cannot be written to", e);
      } finally {
        environment.release();
      }
    } catch (MojoExecutionException e) {
      getLog().error(e.getMessage());
      throw e;
    }
  }

  /**
   * How many flags need to be passed to syntax?
   */
  private void countArgs() {
    numberOfArgs = ARGS;
    
    if (this.margin != -1) numberOfArgs += 2;
    if (this.indent != -1) numberOfArgs += 2;
    if (this.bundleFile != null) numberOfArgs += 2;
    if (this.skeletonFile != null) numberOfArgs += 2;
  }

  /**
   * How many flags need to be passed to syntax?
   */
  private void countFlags() {
    numberOfFlags = 0;

    if (verbose) numberOfFlags++;
    if (debug) numberOfFlags++;
    if (!emitLine) numberOfFlags++;
    if (tokenizer) numberOfFlags++;
  }

  /**
   * How many files need to be sent to syntax?
   * @throws MojoExecutionException
   */
  private void countFiles() throws MojoExecutionException {
    LanguageSupport ls = null;
    
    for (Language l: Language.values()) {
      if (l.support().getId().equalsIgnoreCase(language) ||
          l.support().getLanguageCode().equalsIgnoreCase(language)) {
        ls = l.support();
        break;
      }
    }
    
    if (ls == null) {
      throw new MojoExecutionException("language is not supported");
    }
    numberOfFiles = 0;
    if (sourceFile == null) {
      throw new MojoExecutionException("sourceFile was not provided");
    }
    numberOfFiles++;
    if (outputFile == null) {
      throw new MojoExecutionException("outputFile was not provided");
    }
    numberOfFiles++;
    if (includeFile == null && reportFile != null) {
      includeFile = new File(replaceExtension(outputFile.getAbsolutePath(), ls.getIncludeExtensionSuffix()));
    }
    
    if (includeFile != null) {
      numberOfFiles++;
    }
    if (reportFile != null) {
      numberOfFiles++;
    }
  }

  /**
   * Setup the standard arguments in the argument array
   * @param args is the argument array
   * @return the index from where other items need to be added
   */
  private int setArguments(String[] args) {
    int i = 0;
    args[i++] = "--language";
    args[i++] = this.language;
    args[i++] = "--algorithm";
    args[i++] = this.algorithm;
    args[i++] = "--packing";
    args[i++] = this.packed ? "packed" : "tabular";
    args[i++] = "--external";
    args[i++] = this.externalInclude ? "true" : "false";
    args[i++] = "--driver";
    args[i++] = this.driver;
    if (this.margin != -1) {
      args[i++] = "--margin";
      args[i++] = "" + this.margin;
    }
    if (this.indent != -1) {
      args[i++] = "--indent";
      args[i++] = "" + this.indent;
    }
    if (this.bundleFile != null) {
      args[i++] = "--bundle";
      args[i++] = this.bundleFile.getAbsolutePath();
    }
    if (this.skeletonFile != null) {
      args[i++] = "--skeleton";
      args[i++] = this.skeletonFile.getAbsolutePath();
    }
    return i;
  }

  /**
   * Put the flags in the argument array
   * @param i the current index
   * @param args is the argument array
   * @return the new index
   */
  private int setFlags(int i, String[] args) {
    if (verbose) args[i++] = "--verbose";
    if (debug) args[i++] = "--debug";
    if (!emitLine) args[i++] = "--noline";
    if (tokenizer) args[i++] = "--tokenizer";
    return i;
  }

  /**
   * put the filenames in the argument array
   * @param i the current index
   * @param args is the argument array
   * @return the new index
   */
  private int setFiles(int i, String[] args) {
    args[i++] = sourceFile.getAbsolutePath();
    mkdirs(outputFile);
    args[i++] = outputFile.getAbsolutePath();
    if (includeFile != null) {
      mkdirs(includeFile);
      args[i++] = includeFile.getAbsolutePath();
    }
    if (reportFile != null) {
      mkdirs(reportFile);
      args[i++] = reportFile.getAbsolutePath();
    }
    return i;
  }
  
  private void mkdirs(File file) {
    file.getParentFile().mkdirs();
  }

  /**
   * given a filename, return a new filename with the extension provided
   * @param filename is the input filename
   * @param extension is the new extension
   * @return the new filename
   */
  private String replaceExtension(String filename, String extension) {
    if (filename == null) {
      return null;
    }
    return PathUtils.getFilePathWithSeparator(filename) + PathUtils.getFileNameNoExtension(filename) + extension;
  }

}
