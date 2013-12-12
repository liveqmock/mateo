<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="s" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html>
    <head>
        <title><s:message code="pedidoColportorItem.lista.label" /></title>
    </head>
    <body>
        <jsp:include page="../menu.jsp" >
            <jsp:param name="menu" value="pedidoColportorItem" />
        </jsp:include>

        <h1><s:message code="pedidoColportorItem.lista.label" /></h1><br />
        <div class="page-header">
                <div class="row-fluid">
                    <div class="span4">
                        <b><s:message code="numPedido.label" />:</b>&nbsp;<c:out value="${pedidoColportor.numPedido}" />
                    </div>
                    <div class="span4">
                        <b><s:message code="lugar.label" />:</b>&nbsp;<c:out value="${pedidoColportor.lugar}" />
                    </div>
                    <div class="span4">
                        <b><s:message code="fecha.label" />:</b>&nbsp;<fmt:formatDate pattern="dd/MMM/yyyy" value="${pedidoColportor.fechaPedido}" />
                    </div>
                </div>
                <div class="row-fluid">
                    <div class="span4">
                        <b><s:message code="nombre.label" />:</b>&nbsp;<c:out value="${pedidoColportor.cliente.nombreCompleto}" />
                    </div>
                    <div class="span4">
                        <b><s:message code="razonSocial.label" />:</b>&nbsp;<c:out value="${pedidoColportor.razonSocial}" />
                    </div>
                </div>    
                <div class="row-fluid">
                    <div class="span12">
                        <b><s:message code="observaciones.label" />:</b>&nbsp;<c:out value="${pedidoColportor.observaciones}" />
                    </div>
                </div>
        </div>

        <form name="filtraLista" class="form-search" method="post" action="<c:url value='/colportaje/ventas/items' />">
            <input type="hidden" name="pagina" id="pagina" value="${pagina}" />
            <input type="hidden" name="tipo" id="tipo" value="" />
            <input type="hidden" name="correo" id="correo" value="" />
            <input type="hidden" name="order" id="order" value="${param.order}" />
            <input type="hidden" name="sort" id="sort" value="${param.sort}" />
            <p class="well">
                <c:url var="myurl" value="/colportaje/ventas/items/nuevo"/>
                <a class="btn btn-primary" href="${myurl}">
                    <i class="icon-file icon-white"></i> <s:message code='pedidoColportorItem.nuevo.label' /></a>
                <input name="filtro" type="text" class="input-medium search-query" value="${param.filtro}">
                <button type="submit" class="btn"><i class="icon-search"></i> <s:message code="buscar.label" /></button>
            </p>
            </form>   
            <c:if test="${not empty message}">
                <div class="alert alert-block alert-success fade in" role="status">
                    <a class="close" data-dismiss="alert">×</a>
                    <s:message code="${message}" arguments="${messageAttrs}" />
                </div>
            </c:if>
            <c:if test="${pedidoColportorItem != null}">
                <s:bind path="pedidoColportorItem.*">
                    <c:if test="${not empty status.errorMessages}">
                    <div class="alert alert-block alert-error fade in" role="status">
                        <a class="close" data-dismiss="alert">×</a>
                        <c:forEach var="error" items="${status.errorMessages}">
                            <c:out value="${error}" escapeXml="false"/><br />
                        </c:forEach>
                    </div>
                    </c:if>
                </s:bind>
            </c:if>
            
            <table id="lista" class="table table-striped table-hover">
                <thead>
                    <tr>
                        <jsp:include page="/WEB-INF/jsp/columnaOrdenada.jsp" >
                            <jsp:param name="columna" value="item" />
                        </jsp:include>
                        <jsp:include page="/WEB-INF/jsp/columnaOrdenada.jsp" >
                            <jsp:param name="columna" value="cantidad" />
                        </jsp:include>
                        <jsp:include page="/WEB-INF/jsp/columnaOrdenada.jsp" >
                            <jsp:param name="columna" value="precioUnitario" />
                        </jsp:include>
                        <jsp:include page="/WEB-INF/jsp/columnaOrdenada.jsp" >
                            <jsp:param name="columna" value="observaciones" />
                        </jsp:include>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${pedidoColportorItems}" var="pedidoColportorItem" varStatus="status">
                        <tr class="${status.index % 2 == 0 ? 'even' : 'odd'}">
                            <td><a href="<c:url value='/colportaje/ventas/items/ver/${pedidoColportorItem.id}' />">${pedidoColportorItem.item}</a></td>
                            <td>${pedidoColportorItem.cantidad}</a></td>
                            <td>${pedidoColportorItem.precioUnitario}</a></td>
                            <td>${pedidoColportorItem.observaciones}</a></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            <jsp:include page="/WEB-INF/jsp/paginacion.jsp" />
             
        <content>
            <script src="<c:url value='/js/lista.js' />"></script>
        </content>
    </body>
</html>
