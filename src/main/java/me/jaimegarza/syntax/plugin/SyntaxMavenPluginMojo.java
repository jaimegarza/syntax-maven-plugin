/*
 ===============================================================================
 Copyright (c) 1985, 2012, Jaime Garza
 All rights reserved.
 
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
 * Neither the name of Jaime Garza nor the
       names of its contributors may be used to endorse or promote products
       derived from this software without specific prior written permission.
 
 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
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

import me.jaimegarza.syntax.AnalysisException;
import me.jaimegarza.syntax.OutputException;
import me.jaimegarza.syntax.ParsingException;
import me.jaimegarza.syntax.Syntax;
import me.jaimegarza.syntax.env.Environment;
import me.jaimegarza.syntax.language.Language;
import me.jaimegarza.syntax.language.LanguageSupport;
import me.jaimegarza.syntax.util.PathUtils;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which touches a timestamp file.
 * 
 * @goal generate
 */
public class SyntaxMavenPluginMojo extends AbstractMojo {
  private static final int ARGS = 10;

  /**
   * Name of the source file.
   * 
   * @parameter
   * @required
   */
  private File sourceFile;

  /**
   * Name of the output file to generate.
   * 
   * @parameter
   * @required
   */
  private File outputFile;

  /**
   * Name of the include file to generate.
   * 
   * @parameter
   */
  private File includeFile;

  /**
   * Name of the textual report file with summaries and additional detail.
   * 
   * @parameter
   */
  private File reportFile;

  /**
   * The language to be used: C, pascal, java, ...
   * 
   * @parameter default-value="java"
   */
  private String language;

  /**
   * The algorithm to be used
   * 
   * @parameter default-value="lalr"
   */
  private String algorithm;

  /**
   * Do we provide an include file?
   * 
   * @parameter default-value="false"
   */
  private boolean externalInclude;

  /**
   * Do we provide verbose output?
   * 
   * @parameter default-value="false"
   */
  private boolean verbose;

  /**
   * Do we provide compiler debug output?
   * 
   * @parameter default-value="false"
   */
  private boolean debug;

  /**
   * Do we emit #line numbers on C?
   * 
   * @parameter default-value="true"
   */
  private boolean emitLine;

  /**
   * Is this parser's output packed or tabular
   * 
   * @parameter default-value="packed"
   */
  private boolean packed;

  /**
   * Tell me the driver to be used, either "parser" or "scanner"
   * 
   * @parameter default-value="parser"
   */
  private String driver;

  private int numberOfFlags;
  private int numberOfFiles;

  @Override
  public void execute() throws MojoExecutionException {

    
    countFlags();
    countFiles();

    int i;
    String args[] = new String[ARGS + numberOfFlags + numberOfFiles];
    
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
    }    numberOfFiles = 0;
    if (sourceFile == null) {
      throw new MojoExecutionException("sourceFile was not provided");
    }
    numberOfFiles++;
    if (outputFile == null) {
      throw new MojoExecutionException("outputFile was not provided");
    }
    numberOfFiles++;
    if (includeFile == null && reportFile != null) {
      numberOfFiles++;
      includeFile = new File(replaceExtension(outputFile.getAbsolutePath(), ls.getIncludeExtensionSuffix()));
    } else {
      numberOfFiles++;
    }
    if (reportFile != null) {
      numberOfFiles++;
    }
  }

  /**
   * How many flags need to be passed to syntax?
   */
  private void countFlags() {
    numberOfFlags = 0;

    if (verbose) numberOfFlags++;
    if (debug) numberOfFlags++;
    if (!emitLine) numberOfFlags++;
  }

  /**
   * put the filenames in the argument array
   * @param i the current index
   * @param args is the argument array
   * @return the new index
   */
  private int setFiles(int i, String[] args) {
    args[i++] = sourceFile.getAbsolutePath();
    args[i++] = outputFile.getAbsolutePath();
    if (includeFile != null) {
      args[i++] = includeFile.getAbsolutePath();
    }
    if (reportFile != null) {
      args[i++] = reportFile.getAbsolutePath();
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
    if (verbose) args[i++] = "-v";
    if (debug) args[i++] = "-g";
    if (!emitLine) args[i++] = "-n";
    return i;
  }

  /**
   * Setup the standard arguments in the argument array
   * @param args is the argument array
   * @return the index from where other items need to be added
   */
  private int setArguments(String[] args) {
    args[0] = "--language";
    args[1] = this.language;
    args[2] = "--algorithm";
    args[3] = this.algorithm;
    args[4] = "--packing";
    args[5] = this.packed ? "packed" : "tabular";
    args[6] = "--external";
    args[7] = this.externalInclude ? "true" : "false";
    args[8] = "--driver";
    args[9] = this.driver;
    return ARGS;
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
