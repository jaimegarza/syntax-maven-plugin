Syntax Maven Plugin
=======================================
Please refer to the latest version of syntax for its definition.

This plugin is a group of mojos that allow the execution of syntax in maven

to configure:

      <plugin>
        <groupId>me.jaimegarza</groupId>
        <artifactId>syntax-maven-plugin</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <executions> 
          <execution>
            <id>generate-parser</id>
            <phase>generate-sources</phase>
            <goals>
              <goal>generate</goal>
            </goals>
          </execution>
          <configuration>
          ...
          </configuration>
        </executions>
      </plugin>
      
Once the plug-in is registered in a pom, executing

  mvn syntax:help -Ddetail=true

will provide a description of the configuration options

