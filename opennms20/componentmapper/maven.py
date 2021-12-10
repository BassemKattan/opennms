import glob
import json
import os
import re
from pathlib import Path
from xml.dom import minidom


class MavenIdentifier(object):
    def __init__(self, json_obj):
        self.artifact_id = json_obj['artifactId']
        self.group_id = json_obj['groupId']
        self.version = json_obj['version']
        self.key = "%s:%s:%s" % (self.group_id, self.artifact_id, self.version)

    def matches(self, module):
        return self.artifact_id == module.artifact_id \
               and self.group_id == module.group_id \
               and self.version == module.version


class MavenModule(MavenIdentifier):
    def __init__(self, json_obj, is_root=False):
        super().__init__(json_obj)
        self.pom = json_obj['pom']

        self.path = os.path.abspath(os.path.join(self.pom, '..'))
        self.componentName = []
        self.subcomponentName = []
        self.componentInheritance = True
        self.stability = ""
        self.artifact_id = json_obj['artifactId']
        self.group_id = json_obj['groupId']
        self._is_root = is_root

        # Load pom to get component identifiers
        pomxml = minidom.parse(self.pom)
        elements = pomxml.getElementsByTagName('properties')
        readComponentName = False
        if elements is not None and len(elements)>0:
            properties=elements[0]
            for property in properties.childNodes:
                if property.nodeType == minidom.Node.ELEMENT_NODE:
                    if property.tagName == 'opennms.doc.component':
                        if readComponentName:
                            # Reading another component name without getting a sub between. Add one to keep them in sync
                            self.subcomponentName.append('')
                        self.componentName.append(property.childNodes[0].nodeValue)
                        readComponentName = True
                    elif property.tagName == 'opennms.doc.subcomponent':
                        if not readComponentName:
                            # No component name, so we'll add an empty one to keep them in sync
                            self.componentName.append('')
                        self.subcomponentName.append(property.childNodes[0].nodeValue)
                        readComponentName = False
                    elif property.tagName == 'opennms.doc.stability':
                        self.stability = property.childNodes[0].nodeValue
                    elif property.tagName == 'opennms.doc.componentInheritance':
                        self.componentInheritance = property.childNodes[0].nodeValue

            # If we exit with finding the component name last, make sure to add a corresponding subcomponent
            if readComponentName:
                self.subcomponentName.append('')

        self.dependencies = [MavenIdentifier(obj) for obj in json_obj['dependencies']]


    def __str__(self):
        return self.key

    def is_root(self):
        return self._is_root


class MavenProject(object):
    def __init__(self, graph):
        self.modules = []
        self.modules_by_key = {}
        self.modules_by_relative_path = {}
        self.modules_by_dependencies = {}

        if len(graph) < 1:
            raise Exception("Graph must contain at least one module!")

        # Assume the first module is the root module
        is_root = True
        for module in graph:
            m = MavenModule(module, is_root=is_root)
            # Append to list
            self.modules.append(m)
            # Index by module key
            self.modules_by_key[m.key] = m
            # Only the first is the root
            if is_root:
                self._root_module = m
                is_root = False

        # Index the modules by relative paths
        for module in self.modules:
            # Determine the path of the module relative to the root module
            relative_path = os.path.relpath(module.path, self._root_module.path)
            module.relative_path = relative_path
            self.modules_by_relative_path[relative_path] = module

        # Index the modules by their dependencies
        for module in self.modules:
            for dependency in module.dependencies:
                modules_with_dep = self.modules_by_dependencies.get(dependency.key)
                if modules_with_dep is None:
                    # Create a new set
                    modules_with_dep = set()
                    self.modules_by_dependencies[dependency.key] = modules_with_dep
                modules_with_dep.add(module)

    @staticmethod
    def load(json_file):
        """ Create a new MavenProject from the JSON in the given file """
        with open(json_file) as graph_json:
            graph = json.load(graph_json)
            return MavenProject(graph)



