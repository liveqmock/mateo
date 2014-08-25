<%-- 
    Document   : ver
    Created on : Mar 07, 2013, 6:52:45 AM
    Author     : osoto
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="s" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
    <head>
        <title><s:message code="clienteColportor.ver.label" /></title>
    </head>
    <body>
        <jsp:include page="../menu.jsp" >
            <jsp:param name="menu" value="clienteColportor" />
        </jsp:include>

        <div id="ver-clienteColportor" class="content scaffold-list" role="main">
            <h1><s:message code="clienteColportor.ver.label" /></h1>

            <p class="well">
                <a class="btn btn-primary" href="<s:url value='/colportaje/ventas/clientes'/>">
                    <i class="icon-list icon-white"></i> <s:message code='clienteColportor.lista.label' /></a>
                <a class="btn btn-primary" href="<s:url value='/colportaje/ventas/clientes/nuevo'/>">
                    <i class="icon-file icon-white"></i> <s:message code='clienteColportor.nuevo.label' /></a>
                <a class="btn btn-primary" href="<s:url value='/colportaje/ventas/pedidos/lista/${clienteColportor.id}'/>">
                    <i class="icon-file icon-white"></i> <s:message code='pedidoColportor.lista.label' /></a>
            </p>
            <c:if test="${not empty message}">
                <div class="alert alert-block alert-success fade in" role="status">
                    <a class="close" data-dismiss="alert">×</a>
                    <s:message code="${message}" arguments="${messageAttrs}" />
                </div>
            </c:if>

            <c:url var="eliminaUrl" value="/colportaje/ventas/clientes/elimina" />
            <form:form commandName="clienteColportor" action="${eliminaUrl}" >
                <form:errors path="*" cssClass="alert alert-error" element="ul" />
                <div class="row-fluid" style="padding-bottom: 10px;">
                    <div class="span4">
                        <h4><s:message code="nombre.label" /></h4>
                        <h3>${clienteColportor.nombre}</h3>
                    </div>
                    <div class="span4">
                        <h4><s:message code="apPaterno.label" /></h4>
                        <h3>${clienteColportor.apPaterno}</h3>
                    </div>
                    <div class="span4">
                        <h4><s:message code="apMaterno.label" /></h4>
                        <h3>${clienteColportor.apMaterno}</h3>
                    </div>
                    <div class="span4">
                        <h4><s:message code="telefonoCasa.label" /></h4>
                        <h3>${clienteColportor.telefonoCasa}</h3>
                    </div>
                    <div class="span4">
                        <h4><s:message code="telefonoTrabajo.label" /></h4>
                        <h3>${clienteColportor.telefonoTrabajo}</h3>
                    </div>
                    <div class="span4">
                        <h4><s:message code="telefonoCelular.label" /></h4>
                        <h3>${clienteColportor.telefonoCelular}</h3>
                    </div>
                    <div class="span4">
                        <h4><s:message code="email.label" /></h4>
                        <h3>${clienteColportor.email}</h3>
                    </div>
                    <div class="span4">
                        <h4><s:message code="direccion1.label" /></h4>
                        <h3>${clienteColportor.direccion1}</h3>
                    </div>
                    <div class="span4">
                        <h4><s:message code="direccion2.label" /></h4>
                        <h3>${clienteColportor.direccion2}</h3>
                    </div>
                    <div class="span4">
                        <h4><s:message code="colonia.label" /></h4>
                        <h3>${clienteColportor.colonia}</h3>
                    </div>
                    <div class="span4">
                        <h4><s:message code="municipio.label" /></h4>
                        <h3>${clienteColportor.municipio}</h3>
                    </div>
                    <div class="span4">
                        <h4><s:message code="status.label" /></h4>
                        <h3>${clienteColportor.status}</h3>
                    </div>
                </div>

                <p class="well">
                    <a href="<c:url value='/colportaje/ventas/clientes/edita/${clienteColportor.id}' />" class="btn btn-primary btn-large"><i class="icon-edit icon-white"></i> <s:message code="editar.button" /></a>
                    <form:hidden path="id" />
                    <button type="submit" name="eliminaBtn" class="btn btn-danger btn-large" id="eliminar"  onclick="return confirm('<s:message code="confirma.elimina.message" />');" ><i class="icon-trash icon-white"></i>&nbsp;<s:message code='eliminar.button'/></button>
                </p>
            </form:form>
        </div>
    </body>
</html>
