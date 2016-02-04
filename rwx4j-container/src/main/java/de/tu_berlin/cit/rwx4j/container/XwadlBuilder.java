/**
 * Copyright 2010-2015 Complex and Distributed IT Systems, TU Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tu_berlin.cit.rwx4j.container;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tu_berlin.cit.rwx4j.XmppURI;
import de.tu_berlin.cit.rwx4j.annotations.Consumes;
import de.tu_berlin.cit.rwx4j.annotations.Parameter;
import de.tu_berlin.cit.rwx4j.annotations.Produces;
import de.tu_berlin.cit.rwx4j.annotations.Result;
import de.tu_berlin.cit.rwx4j.annotations.XmppAction;
import de.tu_berlin.cit.rwx4j.annotations.XmppMethod;
import de.tu_berlin.cit.rwx4j.representations.Representation;
import de.tu_berlin.cit.rwx4j.xwadl.DocumentationType;
import de.tu_berlin.cit.rwx4j.xwadl.MethodType;
import de.tu_berlin.cit.rwx4j.xwadl.ParameterType;
import de.tu_berlin.cit.rwx4j.xwadl.XwadlDocument;
import de.tu_berlin.cit.rwx4j.xwadl.ActionDocument.Action;
import de.tu_berlin.cit.rwx4j.xwadl.RequestDocument.Request;
import de.tu_berlin.cit.rwx4j.xwadl.XwadlDocument.Xwadl;
import de.tu_berlin.cit.rwx4j.xwadl.ResponseDocument.Response;

/**
 * TODO
 * 
 * @author Alexander Stanik <alexander.stanik@tu-berlin.de>
 */
public class XwadlBuilder {

	protected final static Logger logger = LoggerFactory.getLogger(XwadlBuilder.class);

	public static XwadlDocument build(String path, ResourceInstance instance) {
		logger.info("Start building xwadl document");
		// create new document
		XwadlDocument xwadl = XwadlDocument.Factory.newInstance();
		// set resource path
		Xwadl resType = xwadl.addNewXwadl();
		resType.setPath(path);
		logger.info("resource path=" + path);

		// search methods
		for (java.lang.reflect.Method method : instance.getClass().getMethods()) {
			// create method entry
			if (method.isAnnotationPresent(XmppMethod.class))
				createMethodXWADL(method, resType.addNewMethod());

			// create action entry
			if (method.isAnnotationPresent(XmppAction.class))
				createActionXWADL(method, resType.addNewAction());
		}

		logger.info("Finished building xwadl document: " + xwadl.toString());
		return xwadl;
	}

	protected static void createMethodXWADL(java.lang.reflect.Method method,
			de.tu_berlin.cit.rwx4j.xwadl.MethodDocument.Method xmlMethod) {
		// set method type
		xmlMethod.setType(MethodType.Enum.forString(method.getAnnotation(XmppMethod.class).value()));
		// set method documentation
		String documentation = method.getAnnotation(XmppMethod.class).documentation();
		if (!documentation.isEmpty()) {
			DocumentationType doc = xmlMethod.addNewDocumentation();
			doc.setTitle(method.getName());
			doc.setStringValue(documentation);
		}
		// logger.info("method=" + method.getName() + " has annotation
		// XmppMethod with value=" + xmlMethod.getType().toString());

		// add request information
		if (method.isAnnotationPresent(Consumes.class)) {
			Request xmlRequest = xmlMethod.addNewRequest();
			xmlRequest.setMediaType(method.getAnnotation(Consumes.class).value());
			Class<? extends Representation> serializer = method.getAnnotation(Consumes.class).serializer();
			try {
				List<Representation> templates = serializer.newInstance().getTemplates();
				if (templates != null) {
					for (Representation rep : templates) {
						xmlRequest.addNewTemplate().setStringValue(rep.toString());
					}
				}
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// add response information
		if (method.isAnnotationPresent(Produces.class)) {
			Response xmlResponse = xmlMethod.addNewResponse();
			xmlResponse.setMediaType(method.getAnnotation(Produces.class).value());
		}
	}

	protected static void createActionXWADL(java.lang.reflect.Method method, Action xmlAction) {
		// set action name
		xmlAction.setName(method.getAnnotation(XmppAction.class).value());
		// set method documentation
		String documentation = method.getAnnotation(XmppAction.class).documentation();
		if (!documentation.isEmpty()) {
			DocumentationType doc = xmlAction.addNewDocumentation();
			doc.setTitle(method.getName());
			doc.setStringValue(documentation);
		}

		// add parameter information
		// Class<?>[] parameterTypes = method.getParameterTypes();
		java.lang.reflect.Parameter[] parameters = method.getParameters();
		for (java.lang.reflect.Parameter parameter : parameters) {
			if (parameter.isAnnotationPresent(Parameter.class)) {
				createParameterXWADL(parameter, xmlAction.addNewParameter());
			}
		}
		// add result information
		if(method.isAnnotationPresent(Result.class)) {
			createParameterXWADL(method, xmlAction.addNewResult());
		}
	}

	private static void createParameterXWADL(java.lang.reflect.Parameter parameter,
			de.tu_berlin.cit.rwx4j.xwadl.ParameterDocument.Parameter xmlParameter) {
		// set parameter name
		Parameter parAnno = parameter.getAnnotation(Parameter.class);
		xmlParameter.setName(parAnno.value());
		// set parameter type
		Class<?> parameterType = parameter.getType();
		if(parameterType.isAssignableFrom(String.class)) {
			xmlParameter.setType(ParameterType.STRING);
		} else if(parameterType.isAssignableFrom(Integer.class)) {
			xmlParameter.setType(ParameterType.INTEGER);
		} else if(parameterType.isAssignableFrom(Double.class)) {
			xmlParameter.setType(ParameterType.DOUBLE);
		} else if(parameterType.isAssignableFrom(Boolean.class)) {
			xmlParameter.setType(ParameterType.BOOLEAN);
		} else if(parameterType.isAssignableFrom(XmppURI.class)) {
			xmlParameter.setType(ParameterType.LINK);
		}
		
		// set parameter documentation
		String documentation = parAnno.documentation();
		if (!documentation.isEmpty()) {
			DocumentationType doc = xmlParameter.addNewDocumentation();
			doc.setTitle(parameter.getName());
			doc.setStringValue(documentation);
		}
	}

	private static void createParameterXWADL(java.lang.reflect.Method method,
			de.tu_berlin.cit.rwx4j.xwadl.ResultDocument.Result xmlResult) {
		// set type
		Class<?> returnType = method.getReturnType();
		if(returnType.isAssignableFrom(String.class)) {
			xmlResult.setType(ParameterType.STRING);
		} else if(returnType.isAssignableFrom(Integer.class)) {
			xmlResult.setType(ParameterType.INTEGER);
		} else if(returnType.isAssignableFrom(Double.class)) {
			xmlResult.setType(ParameterType.DOUBLE);
		} else if(returnType.isAssignableFrom(Boolean.class)) {
			xmlResult.setType(ParameterType.BOOLEAN);
		} else if(returnType.isAssignableFrom(XmppURI.class)) {
			xmlResult.setType(ParameterType.LINK);
		}
		
		// set parameter documentation
		String documentation = method.getAnnotation(Result.class).documentation();
		if (!documentation.isEmpty()) {
			DocumentationType doc = xmlResult.addNewDocumentation();
			doc.setTitle("Return type");
			doc.setStringValue(documentation);
		}
		
	}

}
