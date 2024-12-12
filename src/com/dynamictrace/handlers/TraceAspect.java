package com.dynamictrace.handlers;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;


@Aspect
public class TraceAspect {

    private static TraceAspect instance;
	public static TraceAspect aspectOf() {
	    if (instance == null) {
	        instance = new TraceAspect();
	    }
	    return instance;
	}
	
    private final Logger logger = Logger.getLoggingClient();
	
	@Pointcut("execution(* *(..)) && !within(com.cs.tesis.trace..*) && !within(com.cs.tesis.services..*)")
	public void traceMetodos() {
	}

	@Around("traceMetodos()")
	public Object around(ProceedingJoinPoint pjp) throws Throwable {
		
		String thisID = "";
		if (pjp.getThis() != null) {
			thisID =  pjp.getThis().toString();
		} else {
			thisID = getStaticClassName(pjp.getStaticPart().getSourceLocation().toString());
		}

		String targetId = "-1";
		Object o ;
		if (pjp.getTarget() != null){
			targetId = pjp.getTarget().toString();			
		}
		
		logger.pushLevel("<statements xsi:type=\"trace_Metamodel:MethodCall\" target=\"//" + ObjectId.getObjectId().add(targetId)
				+ "\" caller=\"//" + ObjectId.getObjectId().add(thisID) + "\" actualParameters=\""
				+ getActualParameters(pjp.getArgs()) + "\" name=\"" + getMethodName(pjp.toString()) + "\">");
		
		logger.pushLevel("<subtrace name=\"" + getMethodName(pjp.toString()) + "_trace\">");

		MethodSignature signature = (MethodSignature) pjp.getStaticPart().getSignature();

		for (int i = 0; i < pjp.getArgs().length; ++i) {
			logger.currentLevel("<statements xsi:type=\"trace_Metamodel:FormalParameter\" allocatedObject=\"//"
					+ ObjectId.getObjectId().add(pjp.getArgs()[i].toString()) + "\" name=\"" + signature.getParameterNames()[i] + "\"/>");
		}
		
		System.out.println("Paso");
		try {
            return pjp.proceed();
        } finally {
            //logger.popLevel(signature.toString());
        }

	}

	@AfterReturning(pointcut = "execution(* *(..)) && !within(com.cs.tesis.trace..*) && !within(com.cs.tesis.services..*)", returning = "o")
	public void afterReturning(Object o) {
		
		if (o != null) {
			logger.currentLevel("<statements xsi:type=\"trace_Metamodel:Return\" returnedObject=\"//"
					+ (o != null ? ObjectId.getObjectId().add(o.toString()) : "null") + "\"/>");
		}
		logger.popLevel("</subtrace>");
		logger.popLevel("</statements>");
		
	}

	@After("execution(public static void main(..)) && !within(com.cs.tesis.trace..*) && !within(com.cs.tesis.services..*)")
	public void afterMain() {
		ObjectId.getObjectId().log().forEach(e -> logger.currentLevel(e.toString()));

		logger.popLevel("</trace_Metamodel:ExecutionTrace>");
	}

	private static String getStaticClassName(String s) {
		s = s.substring(0, s.indexOf("."));
		return s + "_static";
	}

	private static String getMethodName(String s) {
		return s.substring(s.lastIndexOf(".") + 1, s.lastIndexOf("("));
	}

	private static String getActualParameters(Object[] args) {
		String log = new String();
		for (int i = 0; i < args.length; ++i) {
			
			log = log.concat(" //" + ObjectId.getObjectId().add(args[i].toString()));
		}
		return log.trim();
	}
	

	

}