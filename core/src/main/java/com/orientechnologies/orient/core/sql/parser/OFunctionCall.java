/* Generated By:JJTree: Do not edit this line. OFunctionCall.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import com.orientechnologies.orient.core.sql.functions.OIndexableSQLFunction;
import com.orientechnologies.orient.core.sql.functions.OSQLFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OFunctionCall extends SimpleNode {

  protected OIdentifier       name;
  protected boolean           star   = false;
  protected List<OExpression> params = new ArrayList<OExpression>();

  public OFunctionCall(int id) {
    super(id);
  }

  public OFunctionCall(OrientSql p, int id) {
    super(p, id);
  }

  /**
   * Accept the visitor. *
   */
  public Object jjtAccept(OrientSqlVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  public boolean isStar() {
    return star;
  }

  public void setStar(boolean star) {
    this.star = star;
  }

  public List<OExpression> getParams() {
    return params;
  }

  public void setParams(List<OExpression> params) {
    this.params = params;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    name.toString(params, builder);
    builder.append("(");
    if (star) {
      builder.append("*");
    } else {
      boolean first = true;
      for (OExpression expr : this.params) {
        if (!first) {
          builder.append(", ");
        }
        expr.toString(params, builder);
        first = false;
      }
    }
    builder.append(")");
  }

  public Object execute(Object targetObjects, OCommandContext ctx) {
    return execute(targetObjects, ctx, name.getValue());
  }

  private Object execute(Object targetObjects, OCommandContext ctx, String name) {
    List<Object> paramValues = new ArrayList<Object>();
    for (OExpression expr : this.params) {
      OIdentifiable currentRecord = (OIdentifiable) ctx.getVariable("$current");
      if(currentRecord == null && targetObjects instanceof OIdentifiable){
        currentRecord = (OIdentifiable) targetObjects; //it happens in case of indexes, drop all this with new executor
      }
      paramValues.add(expr.execute(currentRecord, ctx));
    }
    if (isExpand()) {
      return expanded(targetObjects, ctx, paramValues);
    }
    OSQLFunction function = instantiateOsqlFunction();
    if (function != null && (function.aggregateResults() || function.filterResult())) {
      OSQLFunction statefulFunction = ctx.getAggregateFunction(this);
      if (statefulFunction != null) {
        function = statefulFunction;
      } else {
        ctx.setAggregateFunction(this, function);
      }
    }
    if (function != null) {
      return function.execute(targetObjects, (OIdentifiable) ctx.getVariable("$current"), null, paramValues.toArray(), ctx);
    }
    throw new UnsupportedOperationException("finisho OFunctionCall implementation!");
  }

  private Object expanded(Object targetObjects, OCommandContext ctx, List<Object> paramValues) {
    if (paramValues == null || paramValues.size() != 1) {
      throw new OCommandExecutionException("Invalid " + name + ": wrong number of parameters");
    }
    return paramValues.get(0);
  }

  public static ODatabaseDocumentInternal getDatabase() {
    return ODatabaseRecordThreadLocal.INSTANCE.get();
  }

  public boolean isIndexedFunctionCall() {
    OSQLFunction function = instantiateOsqlFunction();
    return (function instanceof OIndexableSQLFunction);
  }

  /**
   * see OIndexableSQLFunction.searchFromTarget()
   * 
   * @param target
   * @param ctx
   * @param operator
   * @param rightValue
   * @return
   */
  public Iterable<OIdentifiable> executeIndexedFunction(OFromClause target, OCommandContext ctx, OBinaryCompareOperator operator,
      Object rightValue) {
    OSQLFunction function = instantiateOsqlFunction();
    if (function instanceof OIndexableSQLFunction) {
      return ((OIndexableSQLFunction) function).searchFromTarget(target, operator, rightValue, ctx,
          this.getParams().toArray(new OExpression[] { }));
    }
    return null;
  }

  /**
   *
   * @param target
   *          query target
   * @param ctx
   *          execution context
   * @param operator
   *          operator at the right of the function
   * @param rightValue
   *          value to compare to funciton result
   * @return the approximate number of items returned by the condition execution, -1 if the extimation cannot be executed
   */
  public long estimateIndexedFunction(OFromClause target, OCommandContext ctx, OBinaryCompareOperator operator, Object rightValue) {
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name.getValue());
    if (function instanceof OIndexableSQLFunction) {
      return ((OIndexableSQLFunction) function).estimate(target, operator, rightValue, ctx,
          this.getParams().toArray(new OExpression[] {}));
    }
    return -1;
  }

  public boolean isAggregate() {
    if (isExpand()) {
      return false;
    }
    OSQLFunction function = instantiateOsqlFunction();
    return function != null && function.aggregateResults();
  }

  public boolean isFiltering() {
    if (isExpand()) {
      return false;
    }
    OSQLFunction function = instantiateOsqlFunction();
    return function != null && function.filterResult();
  }

  public Object getAggregateResult(OCommandContext ctx) {
    OSQLFunction runtimeFunction = ctx.getAggregateFunction(this);
    if (runtimeFunction == null) {
      runtimeFunction = instantiateOsqlFunction();
    }
    return runtimeFunction.getResult();
  }

  private OSQLFunction instantiateOsqlFunction() {
    OSQLFunction runtimeFunction;
    runtimeFunction = OSQLEngine.getInstance().getFunction(name.getValue());
    runtimeFunction.config(params.toArray());//TODO
    return runtimeFunction;
  }

  public boolean isExpand() {
    // TODO REMOVE THIS!!! it's here only for backward compatibility with the old executor
    String functionName = name.toString().toLowerCase();
    if (functionName.equals("expand") || functionName.equals("flatten")) {
      return true;
    }
    return false;
  }

  public String getDefaultAlias() {
    if (name.toString().equalsIgnoreCase("expand") && params.size() > 0) {
      return params.get(0).toString();
    }
    if (name.toString().equalsIgnoreCase("flatten") && params.size() > 0) {
      return params.get(0).toString();
    }
    return name.toString();
  }
}
/* JavaCC - OriginalChecksum=290d4e1a3f663299452e05f8db718419 (do not edit this line) */
