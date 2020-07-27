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

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.environment_filter_utils.matchers.descriptor.DescriptorMatcher;
import io.jenkins.plugins.environment_filter_utils.matchers.run.RunMatcher;
import jenkins.tasks.filters.EnvVarsFilterException;
import jenkins.tasks.filters.EnvVarsFilterGlobalRule;
import jenkins.tasks.filters.EnvVarsFilterRuleContext;
import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Example of Jenkins global configuration.
 */
public class RegexValueFilter implements EnvVarsFilterGlobalRule {

    private static final Logger LOGGER = Logger.getLogger(RegexValueFilter.class.getName());

    private String regex;

    private List<RunMatcher> exclusions = new ArrayList<>();

    private DescriptorMatcher descriptorMatcher;

    private FilterAction filterAction;

    @DataBoundConstructor
    public RegexValueFilter(String regex, DescriptorMatcher descriptorMatcher, FilterAction filterAction) {
        this.regex = regex;
        this.descriptorMatcher = descriptorMatcher;
        this.filterAction = filterAction;
    }

    public boolean isApplicable(@CheckForNull final Run<?, ?> run, @Nonnull Object o, @Nonnull Launcher launcher) {
        if (descriptorMatcher != null) {
            if (o instanceof Describable) {
                if (!descriptorMatcher.test(((Describable) o).getDescriptor())) {
                    LOGGER.log(Level.CONFIG, o.toString() + " is not one of the globally configured applicable descriptors");
                    return false;
                }
            }
        }

        if (run == null) {
            LOGGER.log(Level.CONFIG, "Run is null for " + launcher + " and: " + o + " so always including it");
            return true;
        }

        if (exclusions != null) {
            if (exclusions.stream().anyMatch(it -> it.test(run))) {
                LOGGER.log(Level.CONFIG, run.toString() + " is being excluded");
                return false;
            }
        }
        LOGGER.log(Level.CONFIG, "No exclusions apply");
        return true;
    }

    @Override
    public void filter(@Nonnull EnvVars envVars, @Nonnull EnvVarsFilterRuleContext envVarsFilterRuleContext) throws EnvVarsFilterException {
        if (regex == null) {
            return;
        }

        Pattern regexp = Pattern.compile(regex);
        final Set<Map.Entry<String, String>> entries = envVars.entrySet();
        final Set<String> envVarsToProcess = new HashSet<>();
        for (Map.Entry<String, String> e : entries) {
            String variableName = e.getKey();
            String variableValue = e.getValue();

            if (regexp.matcher(variableValue).matches()) {
                envVarsToProcess.add(variableName);
            }
        }

        for (String key : envVarsToProcess) {
            filterAction.filter(envVars, key, envVarsFilterRuleContext.getTaskListener());
        }
    }

    public List<RunMatcher> getExclusions() {
        return exclusions;
    }

    public String getRegex() {
        return regex;
    }

    @DataBoundSetter
    public void setExclusions(List<RunMatcher> exclusions) {
        this.exclusions = exclusions;
    }

    public DescriptorMatcher getDescriptorMatcher() {
        return descriptorMatcher;
    }

    public FilterAction getFilterAction() {
        return filterAction;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<EnvVarsFilterGlobalRule> {
        @Override
        public @Nonnull String getDisplayName() {
            return Messages.RegexValueFilter_DisplayName();
        }
    }

    @FunctionalInterface
    public interface FilterActionFunction<T, U, V> {
        void apply(T t, U u, V v) throws EnvVarsFilterException;
    }

    public enum FilterAction {
        REMOVE(RegexValueFilter::remove, Messages._RegexValueFilter_REMOVE_DisplayName()),
        REDACT(RegexValueFilter::redact, Messages._RegexValueFilter_REDACT_DisplayName()),
        FAIL(RegexValueFilter::fail, Messages._RegexValueFilter_FAIL_DisplayName());

        private FilterActionFunction<EnvVars, String, TaskListener> filter;
        private Localizable localizable;

        FilterAction(FilterActionFunction<EnvVars, String, TaskListener> filter, Localizable localizable) {
            this.filter = filter;
            this.localizable = localizable;
        }

        public void filter(EnvVars envVars, String key, TaskListener listener) throws EnvVarsFilterException {
            this.filter.apply(envVars, key, listener);
        }

        public String getDisplayName() {
            return localizable.toString();
        }
    }

    private static void remove(EnvVars envVars, String key, TaskListener listener) {
        listener.getLogger().println(Messages.RegexValueFilter_REMOVE_LogMessage(key));
        envVars.remove(key);
    }
    private static void redact(EnvVars envVars, String key, TaskListener listener) {
        listener.getLogger().println(Messages.RegexValueFilter_REDACT_LogMessage(key));
        envVars.put(key, "REDACTED");
    }
    private static void fail(EnvVars envVars, String key, TaskListener listener) throws EnvVarsFilterException {
        listener.getLogger().println(Messages.RegexValueFilter_FAIL_LogMessage(key));
        throw new EnvVarsFilterException("Environment variable '" + key + "' matched prohibited value");
    }
}
