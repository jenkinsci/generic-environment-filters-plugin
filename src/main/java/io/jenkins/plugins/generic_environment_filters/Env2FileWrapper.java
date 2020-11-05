/*
 * The MIT License
 *
 * Copyright 2020 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.jenkins.plugins.generic_environment_filters;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jenkins.tasks.SimpleBuildWrapper;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public final class Env2FileWrapper extends SimpleBuildWrapper {

    private final List<String> variables;

    @DataBoundConstructor
    public Env2FileWrapper(List<String> variables) {
        this.variables = new ArrayList<>(variables);
    }

    public List<String> getVariables() {
        return variables;
    }

    @Restricted(DoNotUse.class)
    public String getVariablesConjoined() {
        return variables.stream().collect(Collectors.joining("\n"));
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
        List<String> paths = new ArrayList<>();
        for (String variable : variables) {
            String value = initialEnvironment.get(variable);
            if (value != null) {
                FilePath tmp = WorkspaceList.tempDir(workspace);
                if (tmp == null) {
                    throw new AbortException("no workspace");
                }
                FilePath output = tmp.child(variable + ".txt");
                output.write(value, "UTF-8");
                String path = output.getRemote();
                context.env(variable, path);
                listener.getLogger().println("Wrote " + variable + " to " + path);
                paths.add(path);
            }
        }
        context.setDisposer(new DisposerImpl(paths));
    }

    private static final class DisposerImpl extends Disposer {

        private static final long serialVersionUID = 1;
        private final List<String> paths;

        DisposerImpl(List<String> paths) {
            this.paths = paths;
        }

        @Override
        public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
            for (String path : paths) {
                workspace.child(path).delete();
            }
        }

    }

    @Symbol("env2file")
    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public String getDisplayName() {
            return "Save environment variables to files";
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        // TODO JENKINS-27901 standard form control for List<String>
        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new Env2FileWrapper(Arrays.asList(formData.getString("variables").trim().split("\n")));
        }

    }

}
