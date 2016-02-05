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


import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import de.tu_berlin.cit.rwx4j.XmppURI;
import de.tu_berlin.cit.rwx4j.annotations.Consumes;
import de.tu_berlin.cit.rwx4j.annotations.Parameter;
import de.tu_berlin.cit.rwx4j.annotations.Produces;
import de.tu_berlin.cit.rwx4j.annotations.XmppAction;
import de.tu_berlin.cit.rwx4j.annotations.XmppMethod;
import de.tu_berlin.cit.rwx4j.plugin.IContainerPlugin;
import de.tu_berlin.cit.rwx4j.representations.Representation;
import de.tu_berlin.cit.rwx4j.rest.RestDocument;
import de.tu_berlin.cit.rwx4j.rest.ActionDocument.Action;
import de.tu_berlin.cit.rwx4j.rest.MethodDocument.Method;
import de.tu_berlin.cit.rwx4j.xwadl.XwadlDocument;


/**
 * Main container implementation.
 * 
 * @author Alexander Stanik <alexander.stanik@tu-berlin.de>
 */
public class ResourceContainer extends ResourceInstance {

	private final ArrayList<IContainerPlugin> plugins = new ArrayList<IContainerPlugin>();

	/**
	 * Default constructor.
	 * 
	 * @param uri The base URI of this container.
	 */
	public ResourceContainer(XmppURI uri) {
		super(uri.toString());
	}
	
	/**
	 * Adds a new plugin to the container.
	 * 
	 * @param plugin The plugin to add.
	 */
	public void addPlugin(IContainerPlugin plugin) {
		this.plugins.add(plugin);
	}
	
	/**
	 * Generate XWADL document for a particular resource.
	 * 
	 * @param path The path of the resource.
	 * @return Returns the generated XWADL document.
	 */
	public XwadlDocument getXWADL(String path) {
		logger.info("An XWADL is requested for path=" + path);
		// search instance
		ResourceInstance instance = this.getResource(path);
		if(instance == null)
			throw new RuntimeException("Failed: ResourceContainer: "
					+ "Resource not found");
		
		// build xwadl
		XwadlDocument xwadl = XwadlBuilder.build(path, instance);
		// extend xwadl by plugins
		for(IContainerPlugin plugin : this.plugins) {
			plugin.extendXwadl(xwadl, path, instance);
		}
		return xwadl;
	}

	/**
	 * Invoke an operation in order to transfer a resource state.
	 * 
	 * @param xmlRequest The REST request.
	 * @return Returns the REST response.
	 */
	public RestDocument execute(RestDocument xmlRequest) {
		logger.info("An invocation is requested with xml=" + xmlRequest.toString());
		// create response document
		RestDocument xmlResponse = (RestDocument) xmlRequest.copy();
		String path = xmlRequest.getRest().getPath();
		// search instance
		ResourceInstance instance = this.getResource(path);
		if(instance == null)
			throw new RuntimeException("Failed: ResourceContainer: "
					+ "Resource not found");
		
		// invoke method
		if(xmlRequest.getRest().isSetMethod()) {
			try {
				this.invokeMethod(xmlResponse.getRest().getMethod(), instance);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException("Failed: ResourceContainer: "
						+ e.getMessage());
			}
			// remove request part
			if(xmlResponse.getRest().getMethod().isSetRequest()) {
				xmlResponse.getRest().getMethod().unsetRequest();
			}
		}
		
		// invoke action
		if(xmlRequest.getRest().isSetAction()) {
			try {
				this.invokeAction(xmlResponse.getRest().getAction(), instance);
			} catch (URISyntaxException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException("Failed: ResourceContainer: "
						+ e.getMessage());
			}
			// remove request part
			de.tu_berlin.cit.rwx4j.rest.ParameterDocument.Parameter[] params = 
					xmlResponse.getRest().getAction().getParameterArray();
			for(int i = params.length; i >= 0; i--) {
				xmlResponse.getRest().getAction().removeParameter(i);
			}
		}
		
//		logger.info("An invocation was performed and returned is xml=" + xmlResponse.toString());
		return xmlResponse;
	}

	protected void invokeMethod(Method xmlMethod, ResourceInstance instance) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		java.lang.reflect.Method method = this.searchMethod(xmlMethod, instance);
		if(method == null) {
			throw new RuntimeException("Failed: ResourceContainer: "
					+ "Method not found");
		}
		
		// create representations
		Representation input = null;
		if(method.isAnnotationPresent(Consumes.class)) {
			input = method.getAnnotation(Consumes.class).serializer().newInstance();
			input.readRepresentation(xmlMethod.getRequest().getRepresentation());
		}

		// with or without a response
		if(method.isAnnotationPresent(Produces.class)) {
			Representation output = null;
			if(input == null)
				output = (Representation) method.invoke(instance, new Object[] {});
			else
				output = (Representation) method.invoke(instance, input);
		
			StringBuilder builder = new StringBuilder();
			builder = output.writeRepresentation(builder);
			xmlMethod.getResponse().setRepresentation(builder.toString());
		} else {
			if(input == null)
				method.invoke(instance, new Object[] {});
			else
				method.invoke(instance, input);
		}
		
	}

	protected java.lang.reflect.Method searchMethod(Method xmlMethod,
			ResourceInstance instance) {
		String methodType = xmlMethod.getType().toString();
		// search methods
		for(java.lang.reflect.Method method : instance.getClass().getMethods()) {
			// is method of searched type
			if(method.isAnnotationPresent(XmppMethod.class))
				if(methodType.equals(method.getAnnotation(XmppMethod.class).value()))
					if(this.isMethodCorrectAnnotated(xmlMethod, method))
						return method;
		}
		return null;
	}

	protected boolean isMethodCorrectAnnotated(Method xmlMethod,
			java.lang.reflect.Method method) {
		
		// if there is no input
		if(!xmlMethod.isSetRequest() && !method.isAnnotationPresent(Consumes.class)) {
			// if no output
			if(!xmlMethod.isSetResponse() && !method.isAnnotationPresent(Produces.class)) {
				return true;
			} // if both have output
			else if(xmlMethod.isSetResponse() && method.isAnnotationPresent(Produces.class)) {
				if(xmlMethod.getResponse().getMediaType().equals(method.getAnnotation(Produces.class).value()))
					return true;
			}
		} // if both have input
		else if(xmlMethod.isSetRequest() && method.isAnnotationPresent(Consumes.class)) {
			// if both have the same media type
			if(xmlMethod.getRequest().getMediaType().equals(method.getAnnotation(Consumes.class).value())) {
				// if no output
				if(!xmlMethod.isSetResponse() && !method.isAnnotationPresent(Produces.class)) {
					return true;
				} // if both have output
				else if(xmlMethod.isSetResponse() && method.isAnnotationPresent(Produces.class)) {
					if(xmlMethod.getResponse().getMediaType().equals(method.getAnnotation(Produces.class).value()))
						return true;
				}
			}
		} 
		
		// in all other cases
		return false;
	}

	protected void invokeAction(Action xmlAction, ResourceInstance instance) throws URISyntaxException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		java.lang.reflect.Method method = this.searchAction(xmlAction, instance);
		if(method == null) {
			throw new RuntimeException("Failed: ResourceContainer: "
					+ "Action not found");
		}
		
		// create parameters array
		java.lang.reflect.Parameter[] parameters = method.getParameters();
		Object[] params = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			java.lang.reflect.Parameter parameter = parameters[i];
			if (parameter.isAnnotationPresent(Parameter.class)) {
				params[i] = createParameter(parameter, xmlAction.getParameterArray());
			} else {
				throw new RuntimeException("Failed: ResourceContainer: "
						+ "Parameter is not annotated");
			}
		}

		// switch result
		Class<?> returnType = method.getReturnType();
		if(returnType.isAssignableFrom(String.class)) {
			String result = (String) method.invoke(instance, params);
			xmlAction.addNewResult().setSTRING(result);
		} else if(returnType.isAssignableFrom(Integer.class)) {
			Integer result = (Integer) method.invoke(instance, params);
			xmlAction.addNewResult().setINTEGER(result);
		} else if(returnType.isAssignableFrom(Double.class)) {
			Double result = (Double) method.invoke(instance, params);
			xmlAction.addNewResult().setDOUBLE(result);
		} else if(returnType.isAssignableFrom(Boolean.class)) {
			Boolean result = (Boolean) method.invoke(instance, params);
			xmlAction.addNewResult().setBOOLEAN(result);
		} else if(returnType.isAssignableFrom(XmppURI.class)) {
			String result = method.invoke(instance, params).toString();
			xmlAction.addNewResult().setLINK(result);
		} else { // void
			method.invoke(instance, params);
		}

	}

	private Object createParameter(java.lang.reflect.Parameter parameter,
			de.tu_berlin.cit.rwx4j.rest.ParameterDocument.Parameter[] parameterArray) throws URISyntaxException {
		// search parameter by name
		Parameter parAnno = parameter.getAnnotation(Parameter.class);
		String name = parAnno.value();
		for(de.tu_berlin.cit.rwx4j.rest.ParameterDocument.Parameter xmlParameter : parameterArray) {
			if(name.equals(xmlParameter.getName())) {
				// if parameter was found
				Class<?> parameterType = parameter.getType();
				// check type
				if(parameterType.isAssignableFrom(String.class)
						&& xmlParameter.isSetSTRING()) {
					return xmlParameter.getSTRING();
				} else if(parameterType.isAssignableFrom(Integer.class)
						&& xmlParameter.isSetINTEGER()) {
					return new Integer(xmlParameter.getINTEGER());
				} else if(parameterType.isAssignableFrom(Double.class)
						&& xmlParameter.isSetDOUBLE()) {
					return new Double(xmlParameter.getDOUBLE());
				} else if(parameterType.isAssignableFrom(Boolean.class)
						&& xmlParameter.isSetBOOLEAN()) {
					return new Boolean(xmlParameter.getBOOLEAN());
				} else if(parameterType.isAssignableFrom(XmppURI.class)
						&& xmlParameter.isSetLINK()) {
					return new XmppURI(xmlParameter.getLINK());
				}
			}
		}
		
		// set default
		if(!parAnno.defaultValue().isEmpty()) {
			Class<?> parameterType = parameter.getType();
			if(parameterType.isAssignableFrom(String.class)) {
				return parAnno.defaultValue();
			} else if(parameterType.isAssignableFrom(Integer.class)) {
				return new Integer(parAnno.defaultValue());
			} else if(parameterType.isAssignableFrom(Double.class)) {
				return new Double(parAnno.defaultValue());
			} else if(parameterType.isAssignableFrom(Boolean.class)) {
				return new Boolean(parAnno.defaultValue());
			} else if(parameterType.isAssignableFrom(XmppURI.class)) {
				return new XmppURI(parAnno.defaultValue());
			}
		} else {
			throw new RuntimeException("Failed: ResourceContainer: "
					+ "Parameter cannot be localized");
		}

		// this point should never be reached
		return null;
	}

	protected java.lang.reflect.Method searchAction(Action xmlAction,
			ResourceInstance instance) {
		String actionName = xmlAction.getName();
		// search methods
		for(java.lang.reflect.Method method : instance.getClass().getMethods()) {
			// is method of searched type
			if(method.isAnnotationPresent(XmppAction.class))
				if(actionName.equals(method.getAnnotation(XmppAction.class).value()))
					return method;
		}
		return null;
	}
	
}
