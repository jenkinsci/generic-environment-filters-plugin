/*
 * The MIT License
 *
 * Copyright (c) 2020, CloudBees, Inc.
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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Run;
import jenkins.tasks.filters.EnvVarsFilterGlobalRule;
import jenkins.tasks.filters.EnvVarsFilterRuleContext;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class VariableContributingFilter implements EnvVarsFilterGlobalRule {

    private String key;
    private String value;

    @DataBoundConstructor
    public VariableContributingFilter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean isApplicable(Run<?, ?> run, @NonNull Object o, @NonNull Launcher launcher) {
        return true;
    }

    @Override
    public void filter(@NonNull EnvVars envVars, @NonNull EnvVarsFilterRuleContext envVarsFilterRuleContext) {
        envVarsFilterRuleContext.getTaskListener().getLogger().println(Messages.VariableContributingFilter_LogMessage(getKey()));
        envVars.put(getKey(), getValue());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<EnvVarsFilterGlobalRule> {
        @Override
        public @Nonnull
        String getDisplayName() {
            return Messages.VariableContributingFilter_DisplayName();
        }
    }
}
