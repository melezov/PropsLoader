package com.ferega.props.japi;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ResolvablePath {
  private final String base;
  private final String[] partList;

  private final static Pattern ResolvablePattern = Pattern.compile("^(.*)\\$([-\\.\\w]+)\\$(.*)$");
  private static String resolvePart(final Properties props, final String part) {
    final String result;

    final Matcher partMatcher = ResolvablePattern.matcher(part);
    if (partMatcher.matches() && partMatcher.groupCount() == 3) {
      final String prefix = partMatcher.group(1);
      final String key    = partMatcher.group(2);
      final String suffix = partMatcher.group(3);
      final Optional<String> valueOpt = Optional.ofNullable(props.getProperty(key));
      final String value = valueOpt.orElseThrow(() -> new IllegalArgumentException(String.format(
              "An error occured while resolving path part \"%s\": key \"%s\" not found in properties", part, key)));
      result = prefix + value + suffix;
    } else {
      result = part;
    }

    return result;
  }

  public ResolvablePath(final String base, final String ... partList) {
    this.base     = base;
    this.partList = partList;
  }

  public ResolvablePath(final ResolvablePath base, final String ... newPartList) {
    this(base.getBase(),
        Stream.concat(
            Arrays.stream(base.partList),
            Arrays.stream(newPartList)
        ).toArray(String[]::new)
    );
  }

  public static ResolvablePath path(final String path) {
    return path(new File(path));
  }

  public static ResolvablePath path(final File path) {
    final String[] partList = Util.deconstructFile(path);
    final int len = partList.length;
    if (len == 0) {
      throw new IllegalArgumentException("Path must have at least one part!");
    }

    final String head = partList[0];
    final String[] tail = Arrays.copyOfRange(partList, 1, len);
    return new ResolvablePath(head, tail);
  }

  public String getBase() {
    return this.base;
  }

  public String[] getPartList() {
    return this.partList;
  }

  public File toFile() {
    return Util.constructFile(new File(this.base), this.partList);
  }

  public File resolve(final Properties props) {
    final String expandedBase = this.base.startsWith("~")
            ? "$user.home$" + this.base.substring(1)
            : this.base;

    final String resolvedBase = resolvePart(props, expandedBase);
    final String[] resolvedPartList = Arrays.stream(this.partList)
        .map(part -> resolvePart(props, part))
        .toArray(String[]::new);
    return Util.constructFile(new File(resolvedBase), resolvedPartList);
  }

  public File resolve() {
    return resolve(System.getProperties());
  }
}