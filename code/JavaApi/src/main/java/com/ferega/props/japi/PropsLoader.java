package com.ferega.props.japi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

public class PropsLoader {
  private final Map<String, String> propsMap;

  private static List<Properties> resolvePropsList(final Properties resolver, final PropsPath... resolvablePathList) {
      return Arrays.stream(resolvablePathList)
          .map(resolvablePath -> resolvablePath.resolve(resolver))
          .map(file -> Util.loadPropsFromFile(file))
          .collect(Collectors.toList());
  }

  private static Map<String, String> collapseMap(final List<Properties> propsList) {
    final Map<String, String> map = new HashMap<>();
    for (final Properties props : propsList) {
        map.putAll(Util.propsToMap(props));
    }
    return map;
  }

  public PropsLoader(final Map<String, String> propsMap) {
    this.propsMap = Collections.unmodifiableMap(propsMap);
  }

  public PropsLoader(
      final boolean useSystemProps,
      final PropsPath... resolvablePathList) {
    final Properties sysProps = System.getProperties();
    final List<Properties> filePropsList = resolvePropsList(sysProps, resolvablePathList);

    final List<Properties> allPropsList = new ArrayList<Properties>();
    allPropsList.addAll(filePropsList);
    if (useSystemProps) {
      allPropsList.add(sysProps);
    }

    this.propsMap = Collections.unmodifiableMap(collapseMap(allPropsList));
  }

  public PropsLoader addPathList(final PropsPath... newResolvablePathList) {
    if (newResolvablePathList.length != 0) {
      final Map<String, String> allMap = new HashMap<>();

      final Properties sysProps = System.getProperties();
      final List<Properties> filePropsList = resolvePropsList(sysProps, newResolvablePathList);
      final Map<String, String> newPropsMap = collapseMap(filePropsList);

      allMap.putAll(this.propsMap);
      allMap.putAll(newPropsMap);
      return new PropsLoader(allMap);
    } else {
      return this;
    }
  }

  public String get(final String key) {
    try {
      return opt(key).get();
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException(String.format("Key \"%s\" not found in any of the properties collection.", key), e);
    }
  }

  public int getAsInt(final String key) {
    return Integer.parseInt(get(key));
  }

  public long getAsLong(final String key) {
    return Long.parseLong(get(key));
  }

  public boolean getAsBoolean(final String key) {
    return Boolean.parseBoolean(get(key));
  }

  public double getAsDouble(final String key) {
    return Double.parseDouble(get(key));
  }

  public Optional<String> opt(final String key) {
    return Optional.ofNullable(propsMap.get(key));
  }

  public Optional<Integer> optAsInt(final String key) {
    return opt(key).map(Integer::parseInt);
  }

  public Optional<Long> optAsLong(final String key) {
    return opt(key).map(Long::parseLong);
  }

  public Optional<Boolean> optAsBoolean(final String key) {
    return opt(key).map(Boolean::parseBoolean);
  }

  public Optional<Double> optAsDouble(final String key) {
    return opt(key).map(Double::parseDouble);
  }

  public Map<String, String> toMap() {
    return propsMap;
  }

  public Properties toProps() {
    final Properties props = new Properties();
    props.putAll(toMap());
    return props;
  }

  public byte[] toByteArray() {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
          toProps().store(bos, "Created by PropsLoader");
      }
      catch(final IOException e) {
          throw new RuntimeException("Could not write props to output stream", e);
      }
      return bos.toByteArray();
  }

  public ByteArrayInputStream toInputStream() {
    return new ByteArrayInputStream(toByteArray());
  }

  public String toString(final String encoding) {
      try {
          return new String(toByteArray(), encoding);
      }
      catch (final UnsupportedEncodingException e) {
          throw new RuntimeException(e);
      }
  }

  @Override
  public String toString() {
      return toString("ISO-8859-1");
  }

  public PropsLoader select(final String prefix) {
    final String fullPrefix = prefix.endsWith(".") ? prefix : prefix + ".";

    final Map<String, String> selection = new HashMap<>();
    for (final Map.Entry<String, String> entry: toMap().entrySet()) {
      final String key = entry.getKey();
      if (key.startsWith(fullPrefix)) {
        final String val = entry.getValue();
        selection.put(key.substring(fullPrefix.length()), val);
      }
    }
    return new PropsLoader(selection);
  }
}
