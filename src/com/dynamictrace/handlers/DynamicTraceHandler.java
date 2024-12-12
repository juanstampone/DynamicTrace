package com.dynamictrace.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aspectj.weaver.loadtime.WeavingURLClassLoader;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;


import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

public class DynamicTraceHandler extends AbstractHandler {


	private IJavaProject last_check; 

	
	private final Logger logger = Logger.getLoggingClient();
	
	
	public DynamicTraceHandler() {
	
	}
	
	   @Override
	    public Object execute(ExecutionEvent event) throws ExecutionException {
		   try {
  			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		    
			IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer");
						
			if (selection == null)
				selection = (IStructuredSelection) window.getSelectionService().getSelection("org.eclipse.ui.navigator.ProjectExplorer");
			
			Object object = null;
			if (selection != null)
				object = selection.getFirstElement();
			
			IJavaProject project = null;
			if (IJavaProject.class.isInstance(object))
				project = ((IJavaProject) object);
			else if (IProject.class.isInstance(object)) {
				project = JavaCore.create((IProject) object);
			}
			
			if (project == null)
				System.out.println("entro");
			
			if (project != null && project.exists() && !hasCompilationErrors(project)) {
		        
					project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);

		        System.out.println("Proyecto compilado: " + project.getElementName());

		        // Mostrar la carpeta de salida
		        IPath outputLocation = project.getOutputLocation();
		        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		        IPath resolvedLocation = root.getFolder(outputLocation).getLocation();
		        
		        File outputDir = resolvedLocation.toFile();
		        String outputBasePath = outputDir.getAbsolutePath() + File.separator; // Ruta base

		        
		        List<File> classFiles = getAllClassFiles(outputDir);
		        System.out.println("Archivos compilados en: " + resolvedLocation.toOSString());
				
		        
	            Class<?> mainClassName = getMainClass(classFiles,resolvedLocation.toFile());
	            if (mainClassName == null) {
	                MessageDialog.openInformation(null, "Error", "No se encontró la clase principal.");
	                return null;
	            }

	            System.out.println("Clase principal encontrada: " + mainClassName);
		      /*  File outputDir = resolvedLocation.toFile();
		        if (outputDir.exists() && outputDir.isDirectory()) {
		            File[] classFiles = outputDir.listFiles((dir, name) -> name.endsWith(".class"));
		            if (classFiles != null) {
		                for (File classFile : classFiles) {
		                    System.out.println("Archivo compilado: " + classFile.getAbsolutePath());
		                }
		            } else {
		                System.out.println("No se encontraron archivos .class en la carpeta de salida.");
		            }
		        }*/
	            
	            runProgramWithAgent(mainClassName, resolvedLocation.toFile());
	            
	            IWorkbenchWindow windows = HandlerUtil.getActiveWorkbenchWindowChecked(event);
	    		MessageDialog.openInformation(
	    				windows.getShell(),
	    				"DynamicTrace",
	    				"Codigo analizado correctamente");

		  	}
			else if (project == null) 
				MessageDialog.openInformation(null, "Dynamic Trace", "Debe seleccion un proyecto ");
			else if (!project.exists())
				MessageDialog.openInformation(null, "Dynamic Trace", "El proyecto no existe");
			else if (hasCompilationErrors(project))
				MessageDialog.openInformation(null, "Dynamic Trace", "El proyecto tiene errores de compilacion");
			
			return null;
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return null;
		}

		private boolean hasCompilationErrors(IJavaProject javaProject) {
			try {
				IMarker[] problems = javaProject.getProject().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

				for (int problemsIndex = 0; problemsIndex < problems.length; problemsIndex++) {
					if (IMarker.SEVERITY_ERROR == problems[problemsIndex].getAttribute(IMarker.SEVERITY,
							IMarker.SEVERITY_INFO))
						return true;
				}

			} catch (CoreException e) {
				return true;
			}

			return false;
		}
		
		
		private List<File> getAllClassFiles(File directory) {
		    List<File> classFiles = new ArrayList<>();
		    
		    if (directory.exists() && directory.isDirectory()) {
		        // Obtener archivos .class en el directorio actual
		        File[] files = directory.listFiles((dir, name) -> name.endsWith(".class"));
		        if (files != null) {
		            classFiles.addAll(Arrays.asList(files));
		        }

		        // Buscar recursivamente en subdirectorios
		        File[] subDirs = directory.listFiles(File::isDirectory);
		        if (subDirs != null) {
		            for (File subDir : subDirs) {
		                classFiles.addAll(getAllClassFiles(subDir));
		            }
		        }
		    }
		    
		    return classFiles;
		}
		
		  private Class<?> getMainClass(List<File> classFiles, File classpath) {
			  try {
	            for (File classFile : classFiles) {

	                // Obtener el nombre de la clase desde el archivo .class
	                String className = getClassNameFromFile(classFile);

	                if (className != null) {
	                    // Crear un ClassLoader para cargar la clase desde el directorio
	                    URL classUrl = classpath.toURI().toURL();
	                    URL[] classUrls = { classUrl };
	                    URLClassLoader classLoader = new URLClassLoader(classUrls);

	                    // Intentar cargar la clase
	                    Class<?> clazz;
						
							clazz = classLoader.loadClass(className);


	                    // Buscar el método main
	                    Method mainMethod = getMainMethod(clazz);
	                    if (mainMethod != null) {
	                        // Si encontramos el método main, devolver la clase
	                        return clazz;
	                    }
	                }
		        }
		        return null;
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					
					e.printStackTrace();
					return null;
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
		    }
		  
		    // Método que busca el método main en una clase
		    private static Method getMainMethod(Class<?> clazz) {
		        try {
		            // Buscar el método main
		            return clazz.getMethod("main", String[].class);
		        } catch (NoSuchMethodException e) {
		            // Si no tiene el método main, retornamos null
		            return null;
		        }
		    }
		  
		  private static String getClassNameFromFile(File classFile) {
			    // Obtener la ruta completa del archivo .class
			    String path = classFile.getAbsolutePath();
			    
			    // Identificar la base del directorio que contiene los archivos .class
			    String baseDirectory = "bin" + File.separator;  // O ajusta si la base de la carpeta es diferente

			    // Asegurarse de que la ruta contiene el directorio base
			    if (path.contains(baseDirectory)) {
			        // Obtener el nombre de la clase a partir de la ruta
			        String className = path.substring(path.indexOf(baseDirectory) + baseDirectory.length(), path.lastIndexOf(".class"))
			                            .replace(File.separator, ".");

			        // Cargar la clase usando Class.forName()
			        try {
			            Class<?> clazz = Class.forName(className);
			            System.out.println("Clase cargada: " + clazz.getName());

			            // Realizar operaciones con la clase cargada (ejemplo: invocar métodos)
			            // Ejemplo: Obtener todos los métodos de la clase
			            Method[] methods = clazz.getDeclaredMethods();
			            for (Method method : methods) {
			                System.out.println("Método encontrado: " + method.getName());
			            }

			        } catch (ClassNotFoundException e) {
			            System.out.println("Clase no encontrada: " + className);
			        }

			        return className;
			    }
			    // Si no se encuentra la carpeta base, lanzar un error o retornar null
			    System.out.println("Directorio base no encontrado en la ruta.");
			    return null;
		  }
		  
		  private void runProgramWithAgent(Class<?> mainClassName, File outputDir) throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			   
			        // Ruta del directorio de clases

			   /*     String agentPath = "C:\\Users\\Juan\\eclipse-workspace-test\\byteBuddyAgent\\target\\byteBuddyAgent-0.0.1-SNAPSHOT.jar";
			       String weavingPath = "C:\\Users\\Juan\\.m2\\repository\\org\\aspectj\\aspectjweaver\\1.9.19\\aspectjweaver-1.9.19.jar";
			        // Comando para ejecutar el programa con el agente
			        List<String> command = new ArrayList<>();
			        command.add("java"); // Ruta del binario de Java
			        command.add("-javaagent:" + weavingPath); // Agregar el agente
			        command.add("-javaagent:" + agentPath); // Agregar el agente

			        command.add("-cp");
			        command.add(outputDir); // Agregar el classpath
			        command.add(mainClassName.getName()); // Clase principal

			        // Iniciar el proceso
			        ProcessBuilder processBuilder = new ProcessBuilder(command);
		//	        processBuilder.directory(outputDir);
			        processBuilder.inheritIO(); // Pasar I/O al terminal actual
			        Process process = processBuilder.start();

			        // Esperar a que el proceso termine
			        int exitCode = process.waitFor();
			        System.out.println("El programa terminó con código: " + exitCode);
			        

			        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			            String line;
			            while ((line = reader.readLine()) != null) {
			                System.out.println(line); // Captura e imprime la salida estándar
			            }
			        }
			    } catch (Exception e) {
			        e.printStackTrace();
			    }*/
			    	
			  // Configurar el tejedor de AspectJ
	       /*     System.setProperty("aj.weaving.verbose", "true");
	            org.aspectj.weaver.loadtime.Agent.premain("", null); // Inicializa el agente de AspectJ en tiempo de ejecución
	            org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter adapter = new org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter();
	            */
	           // Class<?> clazz = Class.forName(targetClass, true, adapter.());
			  try {
	            URL aspectsUrl = DynamicTraceHandler.class.getProtectionDomain().getCodeSource().getLocation();
	           // URL appUrl = new URL("file:/path/to/your/application.jar");
	            String mainClass = "com.test.Test";
	            mainClass = mainClassName.getName();

	            Bundle bundle = FrameworkUtil.getBundle(DynamicTraceHandler.class);
	            ClassLoader pluginClassLoader = bundle.getClass().getClassLoader();
	            
	        //    URL aspectjWeaverUrl = FileLocator.toFileURL(bundle.getResource("lib/aspectjweaver-1.9.19.jar"));
	          //  URL aspectjRtUrl = FileLocator.toFileURL(bundle.getResource("C:\\Users\\Juan\\eclipse-workspace-test\\mi-primer-plugin\\libaspectjrt-1.9.19.jar"));

	            String path = "C:\\Users\\Juan\\eclipse-workspace-test\\DynamicTrace\\lib\\aspectjrt-1.9.19.jar";

	         // Crear un objeto File desde la ruta
	         File file = new File(path);

	         Logger.getLoggingClient().pushLevel(Logger.MethodHeader);
	         
	         // Crear una URL a partir del archivo
	         URL aspectjRtUrl = file.toURI().toURL();
	            // Configuración del ClassLoader
                URL classUrl = outputDir.toURI().toURL();
                
                
             // Ruta absoluta al archivo aspectjweaver.jar
                String path1 = "C:\\Users\\Juan\\eclipse-workspace-test\\mi-primer-plugin\\lib\\aspectjweaver-1.9.19.jar";

                // Crear un objeto File desde la ruta
                File file1= new File(path1);

                // Crear una URL a partir del archivo
                URL aspectjWeaverUrl = file1.toURI().toURL();
             
             
	            URL[] classURLs = new URL[]{classUrl,aspectsUrl };

	            URL[] aspectURLs = new URL[]{aspectjWeaverUrl,aspectsUrl};
	           ClassLoader defaultClassLoader = Thread.currentThread().getContextClassLoader();
	            ClassLoader weavingClassLoader = new WeavingURLClassLoader(classURLs,aspectURLs, defaultClassLoader);
	            Thread.currentThread().setContextClassLoader(weavingClassLoader);

	            // Carga dinámica de la clase y ejecución
	            Class<?> app = weavingClassLoader.loadClass(mainClass);
	            Method run = app.getMethod("main", String[].class);
	            run.invoke(null, (Object) new String[]{});
	            
	            System.out.println("finn");
	            

                File outputFile = new File("C:\\Users\\Juan\\Desktop\\Logger\\traceprogram.txt");

                
	            // Llamar al método exportLogger.
	            logger.exportLogger(outputFile);
	            

	            
	           /*
			    	// String agentPath = "C:\\Users\\Juan\\eclipse-workspace-test\\byteBuddyAgent\\target\\byteBuddyAgent-0.0.1-SNAPSHOT.jar";
			         String weavingPath = "C:\\Users\\Juan\\eclipse-workspace-test\\mi-primer-plugin\\lib\\aspectjweaver-1.9.19.jar";
			          outputDir = "C:\\Users\\Juan\\runtime-EclipseApplication\\Ejemplo2\\bin";
			          String smainClassName = "com.test.Test";
			          List<String> command = new ArrayList<>();
			          String fullCommand = "java -javaagent:" + weavingPath +
			                     " -cp " + outputDir + " " + smainClassName;
			          ProcessBuilder processBuilder = new ProcessBuilder(fullCommand.split(" "));

			processBuilder.environment().putAll(System.getenv());	
			          // Crear el ProcessBuilder

		          processBuilder.inheritIO(); // Redirigir la entrada/salida al terminal actual7
		          
		          */

			          
			              // Iniciar el proceso
			            //  Process process = processBuilder.start();
			              // Esperar a que el proceso termine
			              //int exitCode = process.waitFor();
			              //System.out.println("Proceso terminado con código: " + exitCode);
			          } catch (IOException e) {
			              e.printStackTrace();
			          }
			          
			       
			    
			    
		  }




}	
