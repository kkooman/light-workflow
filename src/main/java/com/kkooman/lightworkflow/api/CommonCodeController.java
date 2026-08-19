package com.kkooman.lightworkflow.api;

import com.kkooman.lightworkflow.common.CommonCode;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api")
public class CommonCodeController {

    @GetMapping("/common-codes")
    public Map<String, List<Map<String, String>>> getAllCommonCodes() throws IOException, ClassNotFoundException {
        Map<String, List<Map<String, String>>> result = new TreeMap<>();
        for (Class<?> enumClass : findEnumClasses()) {
            result.put(enumClass.getSimpleName(), toCodeEntries(enumClass));
        }
        return result;
    }

    @GetMapping("/common-codes/{enumName}")
    public ResponseEntity<List<Map<String, String>>> getCommonCodes(@PathVariable String enumName)
            throws IOException, ClassNotFoundException {
        for (Class<?> enumClass : findEnumClasses()) {
            if (enumClass.getSimpleName().equalsIgnoreCase(enumName)) {
                return ResponseEntity.ok(toCodeEntries(enumClass));
            }
        }
        return ResponseEntity.notFound().build();
    }

    private List<Class<?>> findEnumClasses() throws IOException, ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        TypeFilter enumFilter = (metadataReader, metadataReaderFactory) -> {
            String className = metadataReader.getClassMetadata().getClassName();
            try {
                Class<?> clazz = Class.forName(className);
                return clazz.isEnum();
            } catch (ClassNotFoundException ex) {
                return false;
            }
        };
        scanner.addIncludeFilter(enumFilter);

        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition beanDefinition : scanner.findCandidateComponents("com.kkooman.lightworkflow")) {
            String className = beanDefinition.getBeanClassName();
            if (className == null) {
                continue;
            }
            Class<?> clazz = Class.forName(className);
            if (clazz.isEnum()) {
                classes.add(clazz);
            }
        }

        classes.sort(Comparator.comparing(Class::getSimpleName));
        return classes;
    }

    private List<Map<String, String>> toCodeEntries(Class<?> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        List<Map<String, String>> entries = new ArrayList<>();
        for (Object constant : constants) {
            Enum<?> enumConstant = (Enum<?>) constant;
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("code", enumConstant.name());
            entry.put("label", extractLabel(enumConstant));
            entries.add(entry);
        }
        return entries;
    }

    private String extractLabel(Enum<?> enumConstant) {
        if (enumConstant instanceof CommonCode commonCode) {
            return commonCode.label();
        }

        String name = enumConstant.name();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
