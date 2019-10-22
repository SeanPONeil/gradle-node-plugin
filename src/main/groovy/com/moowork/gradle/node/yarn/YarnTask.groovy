package com.moowork.gradle.node.yarn

import com.moowork.gradle.node.NodePlugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult

class YarnTask
    extends DefaultTask
{
    protected YarnExecRunner runner

    private Iterable<?> args = []

    private ExecResult result

    public String[] yarnCommand

    public YarnTask()
    {
        this.group = NodePlugin.NODE_GROUP
        this.runner = new YarnExecRunner( this.project )
        dependsOn( YarnSetupTask.NAME )
    }

    void setArgs( final Iterable<?> value )
    {
        this.args = value
    }

    void setYarnCommand( String[] cmd )
    {
        this.yarnCommand = cmd
    }

    @Input
    @Optional
    String [] getYarnCommand()
    {
        return yarnCommand
    }

    @Input
    @Optional
    Iterable<?> getArgs()
    {
        return this.args
    }

    @Nested
    YarnExecRunner getExecRunner()
    {
        return this.runner
    }

    void setEnvironment( final Map<String, ?> value )
    {
        this.runner.environment << value
    }

    void setWorkingDir( final File workingDir )
    {
        this.runner.workingDir = workingDir
    }

    void setIgnoreExitValue( final boolean value )
    {
        this.runner.ignoreExitValue = value
    }

    void setExecOverrides( final Closure closure )
    {
        this.runner.execOverrides = closure
    }

    @Internal
    ExecResult getResult()
    {
        return this.result
    }

    @TaskAction
    void exec()
    {
        if ( this.yarnCommand != null )
        {
            this.runner.arguments.addAll( this.yarnCommand )
        }

        this.runner.arguments.addAll( this.args )
        this.result = this.runner.execute()
    }
}
