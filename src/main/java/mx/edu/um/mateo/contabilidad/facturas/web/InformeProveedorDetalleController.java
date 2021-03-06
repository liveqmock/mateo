/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.edu.um.mateo.contabilidad.facturas.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import mx.edu.um.mateo.contabilidad.facturas.model.Contrarecibo;
import mx.edu.um.mateo.contabilidad.facturas.model.ContrareciboVO;
import mx.edu.um.mateo.contabilidad.facturas.model.InformeProveedor;
import mx.edu.um.mateo.contabilidad.facturas.model.InformeProveedorDetalle;
import mx.edu.um.mateo.contabilidad.facturas.model.ProveedorFacturas;
import mx.edu.um.mateo.contabilidad.facturas.service.ContrareciboManager;
import mx.edu.um.mateo.contabilidad.facturas.service.InformeProveedorDetalleManager;
import mx.edu.um.mateo.contabilidad.facturas.service.InformeProveedorManager;
import mx.edu.um.mateo.general.dao.ProveedorDao;
import mx.edu.um.mateo.general.model.UploadFileForm;
import mx.edu.um.mateo.general.model.Usuario;
import mx.edu.um.mateo.general.utils.BancoNoCoincideException;
import mx.edu.um.mateo.general.utils.ClabeNoCoincideException;
import mx.edu.um.mateo.general.utils.Constantes;
import mx.edu.um.mateo.general.utils.CuentaChequeNoCoincideException;
import mx.edu.um.mateo.general.utils.FacturaRepetidaFFacturaException;
import mx.edu.um.mateo.general.utils.FacturaRepetidaFFiscalException;
import mx.edu.um.mateo.general.utils.FormaPagoNoCoincideException;
import mx.edu.um.mateo.general.utils.ProveedorNoCoincideException;
import mx.edu.um.mateo.general.utils.ReporteException;
import mx.edu.um.mateo.general.web.BaseController;
import mx.edu.um.mateo.inscripciones.model.FileUploadForm;
import mx.edu.um.mateo.rh.model.Empleado;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.annotations.ReadOnly;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author develop
 */
@Controller
@RequestMapping(Constantes.PATH_INFORMEPROVEEDOR_DETALLE)
public class InformeProveedorDetalleController extends BaseController {

    @Autowired
    private InformeProveedorDetalleManager manager;
    @Autowired
    private InformeProveedorManager managerInforme;
    @Autowired
    private ProveedorDao proveedorDao;
    @Autowired
    private ContrareciboManager contrareciboManager;

//    @PreAuthorize("hasAnyRole('ROLE_PRV_USER')")
    @RequestMapping({"", "/lista"})
    public String lista(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) Long pagina,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String sort,
            Usuario usuario,
            Errors errors,
            Model modelo) {
        log.debug("Mostrando lista de informes");
        Map<String, Object> params = new HashMap<>();
        Long empresaId = (Long) request.getSession().getAttribute("empresaId");
//        params.put("empresa", empresaId);
        params.put(Constantes.CONTAINSKEY_REPORTE, Constantes.CONTAINSKEY_REPORTE);
        InformeProveedor informeId = (InformeProveedor) request.getSession().getAttribute("informeId");
        params.put("informeProveedor", informeId.getId());
        params.put("statusInforme", informeId.getStatus());

        if (StringUtils.isNotBlank(filtro)) {
            params.put(Constantes.CONTAINSKEY_FILTRO, filtro);
        }
        if (pagina != null) {
            params.put(Constantes.CONTAINSKEY_PAGINA, pagina);
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        } else {
            pagina = 1L;
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        }
        if (StringUtils.isNotBlank(order)) {
            params.put(Constantes.CONTAINSKEY_ORDER, order);
            params.put(Constantes.CONTAINSKEY_SORT, sort);
        }

        if (StringUtils.isNotBlank(tipo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, true);
            params = manager.lista(params);
            try {
                generaReporte(tipo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        response, "contrarecibo", Constantes.EMP, empresaId);
                return null;
            } catch (ReporteException e) {
                log.error("No se pudo generar el reporte", e);
                params.remove(Constantes.CONTAINSKEY_REPORTE);
                //errors.reject("error.generar.reporte");
            }
        }

        if (StringUtils.isNotBlank(correo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, true);
            params = manager.lista(params);

            params.remove(Constantes.CONTAINSKEY_REPORTE);
            try {
                enviaCorreo(correo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        request, "contrarecibo", Constantes.EMP, empresaId);
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE, "lista.enviada.message");
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{messageSource.getMessage("detalle.lista.label", null, request.getLocale()), ambiente.obtieneUsuario().getUsername()});
            } catch (ReporteException e) {
                log.error("No se pudo enviar el reporte por correo", e);
            }
        }
        params = manager.lista(params);
        log.debug("params{}", params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));
        modelo.addAttribute(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));

        pagina(params, modelo, Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, pagina);

        if (usuario.isEmpleado()) {
            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_LISTAEMP;
        }
        return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_LISTA;

    }

    /**
     * Listado de detalles del contrarecibo
     *
     * @param request
     * @param response
     * @param filtro
     * @param pagina
     * @param tipo
     * @param correo
     * @param order
     * @param sort
     * @param usuario
     * @param errors
     * @param modelo
     * @return
     */
//    @PreAuthorize("hasAnyRole('ROLE_PRV_')")
    @RequestMapping("/listaContrarecibo")
    public String listaContrarecibos(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) Long pagina,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String sort,
            Usuario usuario,
            Errors errors,
            Model modelo) {
        log.debug("Mostrando lista de informes");
        Map<String, Object> params = new HashMap<>();
        Long empresaId = (Long) request.getSession().getAttribute("empresaId");
//        params.put("empresa", empresaId);
        Contrarecibo contrareciboId = (Contrarecibo) request.getSession().getAttribute("contrareciboId");
        params.put("contrarecibo", contrareciboId.getId());
        params.put(Constantes.CONTAINSKEY_REPORTE, Constantes.CONTAINSKEY_REPORTE);
        if (StringUtils.isNotBlank(filtro)) {
            params.put(Constantes.CONTAINSKEY_FILTRO, filtro);
        }
        if (pagina != null) {
            params.put(Constantes.CONTAINSKEY_PAGINA, pagina);
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        } else {
            pagina = 1L;
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        }
        if (StringUtils.isNotBlank(order)) {
            params.put(Constantes.CONTAINSKEY_ORDER, order);
            params.put(Constantes.CONTAINSKEY_SORT, sort);
        }

        if (StringUtils.isNotBlank(tipo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, true);
            params = manager.contrarecibo(params);
            try {
                generaReporte(tipo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        response, "contrarecibo", Constantes.EMP, empresaId);
                return null;
            } catch (ReporteException e) {
                log.error("No se pudo generar el reporte", e);
                params.remove(Constantes.CONTAINSKEY_REPORTE);
                //errors.reject("error.generar.reporte");
            }
        }

        if (StringUtils.isNotBlank(correo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, true);
            params = manager.contrarecibo(params);

            params.remove(Constantes.CONTAINSKEY_REPORTE);
            try {
                enviaCorreo(correo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        request, "contrarecibo", Constantes.EMP, empresaId);
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE, "lista.enviada.message");
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{messageSource.getMessage("detalle.lista.label", null, request.getLocale()), ambiente.obtieneUsuario().getUsername()});
            } catch (ReporteException e) {
                log.error("No se pudo enviar el reporte por correo", e);
            }
        }
        params = manager.contrarecibo(params);
        log.debug("params{}", params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));
        modelo.addAttribute(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));
        pagina(params, modelo, Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, pagina);

        return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_LISTACONTRARECIBOS;
    }

    /**
     * Listado de contrarecibos
     *
     * @param request
     * @param response
     * @param filtro
     * @param pagina
     * @param tipo
     * @param correo
     * @param order
     * @param sort
     * @param usuario
     * @param errors
     * @param modelo
     * @return
     */
    @RequestMapping("/contrarecibos")
    public String contrarecibos(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) Long pagina,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String sort,
            Usuario usuario,
            Errors errors,
            Model modelo) {
        log.debug("Mostrando lista de informes");
        Map<String, Object> params = new HashMap<>();
        Long empresaId = (Long) request.getSession().getAttribute("empresaId");
        params.put("empresa", empresaId);
        params.put(Constantes.CONTAINSKEY_REPORTE, Constantes.CONTAINSKEY_REPORTE);
        if (StringUtils.isNotBlank(filtro)) {
            params.put(Constantes.CONTAINSKEY_FILTRO, filtro);
        }
        if (pagina != null) {
            params.put(Constantes.CONTAINSKEY_PAGINA, pagina);
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        } else {
            pagina = 1L;
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        }
        if (StringUtils.isNotBlank(order)) {
            params.put(Constantes.CONTAINSKEY_ORDER, order);
            params.put(Constantes.CONTAINSKEY_SORT, sort);
        }

        if (StringUtils.isNotBlank(tipo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, true);
            params = manager.lista(params);
            try {
                generaReporte(tipo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        response, "contrarecibo", Constantes.EMP, empresaId);
                return null;
            } catch (ReporteException e) {
                log.error("No se pudo generar el reporte", e);
                params.remove(Constantes.CONTAINSKEY_REPORTE);
                //errors.reject("error.generar.reporte");
            }
        }

        if (StringUtils.isNotBlank(correo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, true);
            params = manager.lista(params);

            params.remove(Constantes.CONTAINSKEY_REPORTE);
            try {
                enviaCorreo(correo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        request, "contrarecibo", Constantes.EMP, empresaId);
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE, "lista.enviada.message");
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{messageSource.getMessage("detalle.lista.label", null, request.getLocale()), ambiente.obtieneUsuario().getUsername()});
            } catch (ReporteException e) {
                log.error("No se pudo enviar el reporte por correo", e);
            }
        }
        params = contrareciboManager.lista(params);
        //log.debug("params{}", params.get(Constantes.CONTAINSKEY_CONTRARECIBOS));
        modelo.addAttribute(Constantes.CONTAINSKEY_CONTRARECIBOS, params.get(Constantes.CONTAINSKEY_CONTRARECIBOS));

        pagina(params, modelo, Constantes.CONTAINSKEY_CONTRARECIBOS, pagina);
        return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_CONTRARECIBOS;
    }

    /**
     * Listado de facturadas enviadas por el proveedor hacia la UM organizadas
     * por encabezado
     *
     * @param request
     * @param response
     * @param filtro
     * @param pagina
     * @param tipo
     * @param correo
     * @param order
     * @param sort
     * @param usuario
     * @param errors
     * @param modelo
     * @return
     */
    @RequestMapping({"/contrarecibo"})
    public String contrarecibo(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) Long pagina,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String sort,
            Usuario usuario,
            Errors errors,
            Model modelo) {
        log.debug("Entrando a contrarecibo..**..");
        Map<String, Object> params = new HashMap<>();
        Long empresaId = (Long) request.getSession().getAttribute("empresaId");
//        params.put("empresa", empresaId);
        params.put(Constantes.CONTAINSKEY_REPORTE, Constantes.CONTAINSKEY_REPORTE);
        InformeProveedor informeId = (InformeProveedor) request.getSession().getAttribute("informeId");
        params.put("informeProveedor", informeId.getId());
        if (StringUtils.isNotBlank(filtro)) {
            params.put(Constantes.CONTAINSKEY_FILTRO, filtro);
        }
        if (pagina != null) {
            params.put(Constantes.CONTAINSKEY_PAGINA, pagina);
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        } else {
            pagina = 1L;
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        }
        if (StringUtils.isNotBlank(order)) {
            params.put(Constantes.CONTAINSKEY_ORDER, order);
            params.put(Constantes.CONTAINSKEY_SORT, sort);
        }

        if (StringUtils.isNotBlank(tipo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, true);
            params = manager.lista(params);
            try {
                generaReporte(tipo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        response, "contrarecibo", Constantes.EMP, empresaId);
                return null;
            } catch (ReporteException e) {
                log.error("No se pudo generar el reporte", e);
                params.remove(Constantes.CONTAINSKEY_REPORTE);
                //errors.reject("error.generar.reporte");
            }
        }

        if (StringUtils.isNotBlank(correo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, true);
            params = manager.lista(params);

            params.remove(Constantes.CONTAINSKEY_REPORTE);
            try {
                enviaCorreo(correo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        request, "contrarecibo", Constantes.EMP, empresaId);
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE, "lista.enviada.message");
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{messageSource.getMessage("detalle.lista.label", null, request.getLocale()), ambiente.obtieneUsuario().getUsername()});
            } catch (ReporteException e) {
                log.error("No se pudo enviar el reporte por correo", e);
            }
        }
        params = manager.lista(params);
        log.debug("params{}", params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));
        modelo.addAttribute(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));

        pagina(params, modelo, Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, pagina);
        return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_CONTRARECIBO;
    }

    @PreAuthorize("hasRole('ROLE_PRV_VALIDA')")
    @RequestMapping("/revisar")
    public String revisar(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) Long pagina,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String sort,
            Usuario usuario,
            Errors errors,
            Model modelo) {
        log.debug("Entrando a revisar..**..");
        Map<String, Object> params = new HashMap<>();
        Long empresaId = (Long) request.getSession().getAttribute("empresaId");
        params.put("empresa", empresaId);
        params.put(Constantes.CONTAINSKEY_REPORTE, Constantes.CONTAINSKEY_REPORTE);
//        InformeProveedor informeId = (InformeProveedor) request.getSession().getAttribute("informeId");
//        params.put("informeProveedor", informeId.getId());
        if (StringUtils.isNotBlank(filtro)) {
            params.put(Constantes.CONTAINSKEY_FILTRO, filtro);
        }
        if (pagina != null) {
            params.put(Constantes.CONTAINSKEY_PAGINA, pagina);
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        } else {
            pagina = 1L;
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        }
        if (StringUtils.isNotBlank(order)) {
            params.put(Constantes.CONTAINSKEY_ORDER, order);
            params.put(Constantes.CONTAINSKEY_SORT, sort);
        }

        if (StringUtils.isNotBlank(tipo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, Constantes.CONTAINSKEY_REPORTE);
            params = manager.revisar(params);
            try {
                generaReporte(tipo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        response, "contrarecibo", Constantes.EMP, empresaId);
                return null;
            } catch (ReporteException e) {
                log.error("No se pudo generar el reporte", e);
                params.remove(Constantes.CONTAINSKEY_REPORTE);
                //errors.reject("error.generar.reporte");
            }
        }

        if (StringUtils.isNotBlank(correo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, true);
            params = manager.revisar(params);

            params.remove(Constantes.CONTAINSKEY_REPORTE);
            try {
                enviaCorreo(correo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        request, "contrarecibo", Constantes.EMP, empresaId);
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE, "lista.enviada.message");
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{messageSource.getMessage("detalle.lista.label", null, request.getLocale()), ambiente.obtieneUsuario().getUsername()});
            } catch (ReporteException e) {
                log.error("No se pudo enviar el reporte por correo", e);
            }
        }
        params = manager.revisar(params);
        log.debug("params{}", params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));
        modelo.addAttribute(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));

        pagina(params, modelo, Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, pagina);

        return "/factura/informeProveedorDetalle/detalles";
    }

    @PreAuthorize("hasRole('ROLE_PRV_COMPRAS')")
    @RequestMapping("/revisarFacturasCompras")
    public String revisarFacturasCompras(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) Long pagina,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String sort,
            Usuario usuario,
            Errors errors,
            Model modelo) {
        log.debug("Entrando a revisar..**..");
        Map<String, Object> params = new HashMap<>();
        Long empresaId = (Long) request.getSession().getAttribute("empresaId");
        params.put("empresa", empresaId);
        params.put(Constantes.CONTAINSKEY_REPORTE, Constantes.CONTAINSKEY_REPORTE);
//        InformeProveedor informeId = (InformeProveedor) request.getSession().getAttribute("informeId");
//        params.put("informeProveedor", informeId.getId());
        if (StringUtils.isNotBlank(filtro)) {
            params.put(Constantes.CONTAINSKEY_FILTRO, filtro);
        }
        if (pagina != null) {
            params.put(Constantes.CONTAINSKEY_PAGINA, pagina);
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        } else {
            pagina = 1L;
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        }
        if (StringUtils.isNotBlank(order)) {
            params.put(Constantes.CONTAINSKEY_ORDER, order);
            params.put(Constantes.CONTAINSKEY_SORT, sort);
        }

        if (StringUtils.isNotBlank(tipo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, Constantes.CONTAINSKEY_REPORTE);
            params = manager.revisar(params);
            try {
                generaReporte(tipo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        response, "contrarecibo", Constantes.EMP, empresaId);
                return null;
            } catch (ReporteException e) {
                log.error("No se pudo generar el reporte", e);
                params.remove(Constantes.CONTAINSKEY_REPORTE);
                //errors.reject("error.generar.reporte");
            }
        }

        if (StringUtils.isNotBlank(correo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, true);
            params = manager.revisar(params);

            params.remove(Constantes.CONTAINSKEY_REPORTE);
            try {
                enviaCorreo(correo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        request, "contrarecibo", Constantes.EMP, empresaId);
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE, "lista.enviada.message");
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{messageSource.getMessage("detalle.lista.label", null, request.getLocale()), ambiente.obtieneUsuario().getUsername()});
            } catch (ReporteException e) {
                log.error("No se pudo enviar el reporte por correo", e);
            }
        }
        params = manager.revisar(params);
        log.debug("params{}", params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));
        modelo.addAttribute(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));

        pagina(params, modelo, Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, pagina);

        return "/factura/informeProveedorDetalle/revisarFacturasCompras";
    }

    @PreAuthorize("hasRole('ROLE_PRV_GENERA')")
    @RequestMapping("/listaRevisados")
    public String revisados(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) Long pagina,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String sort,
            Usuario usuario,
            Errors errors,
            Model modelo) {
        log.debug("Entrando a revisar..**..");
        Map<String, Object> params = new HashMap<>();
        Long empresaId = (Long) request.getSession().getAttribute("empresaId");
        params.put("empresa", empresaId);
        params.put(Constantes.CONTAINSKEY_REPORTE, Constantes.CONTAINSKEY_REPORTE);
//        InformeProveedor informeId = (InformeProveedor) request.getSession().getAttribute("informeId");
//        params.put("informeProveedor", informeId.getId());
        if (StringUtils.isNotBlank(filtro)) {
            params.put(Constantes.CONTAINSKEY_FILTRO, filtro);
        }
        if (pagina != null) {
            params.put(Constantes.CONTAINSKEY_PAGINA, pagina);
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        } else {
            pagina = 1L;
            modelo.addAttribute(Constantes.CONTAINSKEY_PAGINA, pagina);
        }
        if (StringUtils.isNotBlank(order)) {
            params.put(Constantes.CONTAINSKEY_ORDER, order);
            params.put(Constantes.CONTAINSKEY_SORT, sort);
        }

        if (StringUtils.isNotBlank(tipo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, Constantes.CONTAINSKEY_REPORTE);
            params = manager.listaRevisados(params);
            try {
                generaReporte(tipo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        response, "contrarecibo", Constantes.EMP, empresaId);
                return null;
            } catch (ReporteException e) {
                log.error("No se pudo generar el reporte", e);
                params.remove(Constantes.CONTAINSKEY_REPORTE);
                //errors.reject("error.generar.reporte");
            }
        }

        if (StringUtils.isNotBlank(correo)) {
            params.put(Constantes.CONTAINSKEY_REPORTE, true);
            params = manager.listaRevisados(params);

            params.remove(Constantes.CONTAINSKEY_REPORTE);
            try {
                enviaCorreo(correo, (List<InformeProveedorDetalle>) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE),
                        request, "contrarecibo", Constantes.EMP, empresaId);
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE, "lista.enviada.message");
                modelo.addAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{messageSource.getMessage("detalle.lista.label", null, request.getLocale()), ambiente.obtieneUsuario().getUsername()});
            } catch (ReporteException e) {
                log.error("No se pudo enviar el reporte por correo", e);
            }
        }
        params = manager.listaRevisados(params);
        log.debug("params{}", params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));
        modelo.addAttribute(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE));

        pagina(params, modelo, Constantes.CONTAINSKEY_INFORMESPROVEEDOR_DETALLE, pagina);

        return "/factura/informeProveedorDetalle/revisados";
    }

    @RequestMapping("/ver/{id}")
    public String ver(HttpServletRequest request, @PathVariable Long id, Model modelo) {
        log.debug("Mostrando paquete {}", id);
        InformeProveedorDetalle detalle = manager.obtiene(id);
        request.getSession().setAttribute("detalleUpdateFileId", id);
        modelo.addAttribute(Constantes.ADDATTRIBUTE_INFORMEPROVEEDOR_DETALLE, detalle);

        return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_VER;
    }

    @RequestMapping("/verContrarecibo/{id}")
    public String verContrarecibo(HttpServletRequest request, @PathVariable Long id, Model modelo) {
        log.debug("Mostrando paquete {}", id);
        Contrarecibo contrarecibo = contrareciboManager.obtiene(id);

        modelo.addAttribute(Constantes.ADDATTRIBUTE_CONTRARECIBO, contrarecibo);
        request.getSession().setAttribute("contrareciboId", contrarecibo);

        return "redirect:" + Constantes.PATH_INFORMEPROVEEDOR_DETALLE_LISTACONTRARECIBOS;
    }

    @RequestMapping("/nuevo")
    public String nuevo(HttpServletRequest request, Model modelo) {
        log.debug("Nuevo paquete");
        Usuario usuario = ambiente.obtieneUsuario();
        if (usuario.isProveedor()) {
            ProveedorFacturas proveedorFacturas = (ProveedorFacturas) usuario;
            modelo.addAttribute("proveedorFacturas", proveedorFacturas);
            request.getSession().setAttribute("proveedorLogeado", proveedorFacturas);
        }
        Map<String, Object> params = new HashMap<>();

        InformeProveedorDetalle detalle = new InformeProveedorDetalle();
        modelo.addAttribute(Constantes.ADDATTRIBUTE_INFORMEPROVEEDOR_DETALLE, detalle);
        params.put("empresa", request.getSession()
                .getAttribute("empresaId"));
        params.put("reporte", true);
        modelo.addAttribute(Constantes.ADDATTRIBUTE_INFORMEPROVEEDOR_DETALLE, detalle);
        return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_NUEVO;
    }

    @RequestMapping("/nueva")
    public String nueva(HttpServletRequest request, Model modelo) {
        log.debug("Nuevo paquete");
        Usuario usuario = ambiente.obtieneUsuario();
        if (usuario.isProveedor()) {
            ProveedorFacturas proveedorFacturas = (ProveedorFacturas) usuario;
            modelo.addAttribute("proveedorFacturas", proveedorFacturas);
            request.getSession().setAttribute("proveedorLogeado", proveedorFacturas);
        }
        Map<String, Object> params = new HashMap<>();

        InformeProveedorDetalle detalle = new InformeProveedorDetalle();
        modelo.addAttribute(Constantes.ADDATTRIBUTE_INFORMEPROVEEDOR_DETALLE, detalle);
        params.put("empresa", request.getSession()
                .getAttribute("empresaId"));
        params.put("reporte", true);
        modelo.addAttribute(Constantes.ADDATTRIBUTE_INFORMEPROVEEDOR_DETALLE, detalle);
        return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_NUEVA;
    }

    @Transactional
    @RequestMapping(value = "/graba", method = RequestMethod.POST)
    public String graba(HttpServletRequest request, HttpServletResponse response, @Valid InformeProveedorDetalle detalle,
            BindingResult bindingResult, Errors errors, Model modelo, RedirectAttributes redirectAttributes,
            @ModelAttribute("uploadFileForm") FileUploadForm uploadForm) throws Exception {
        for (String nombre : request.getParameterMap().keySet()) {
            log.debug("Param: {} : {}", nombre, request.getParameterMap().get(nombre));
        }
        if (bindingResult.hasErrors()) {
            log.debug("Hubo algun error en la forma, regresando");
            Map<String, Object> params = new HashMap<>();
            params.put("empresa", request.getSession()
                    .getAttribute("empresaId"));

            this.despliegaBindingResultErrors(bindingResult);

            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_NUEVO;
        }

        Map<String, Object> params = new HashMap<>();
        //Subir archivos
        try {
            List<MultipartFile> files = new ArrayList<>();
            files.add(detalle.getFile());
            files.add(detalle.getFile2());

            List<String> fileNames = new ArrayList<String>();
            Calendar calendar = GregorianCalendar.getInstance();
            int año = calendar.get(Calendar.YEAR);
            int mes = calendar.get(Calendar.MONTH);
            int dia = calendar.get(Calendar.DATE);
            log.debug("sizefiles{}", files.size());
            if (null != files && files.size() > 0) {
                for (MultipartFile multipartFile : files) {
                    String fileName = multipartFile.getOriginalFilename();
                    log.debug("filename{}", fileName);
                    fileNames.add(fileName);
                    String uploadDir = "/home/facturas/" + año + "/" + mes + "/" + dia + "/" + request.getRemoteUser() + "/" + multipartFile.getOriginalFilename();
                    File dirPath = new File(uploadDir);
                    if (!dirPath.exists()) {
                        dirPath.mkdirs();
                    }
                    multipartFile.transferTo(new File("/home/facturas/" + año + "/" + mes + "/" + dia + "/" + request.getRemoteUser() + "/" + multipartFile.getOriginalFilename()));
                    if (multipartFile.getOriginalFilename().contains(".pdf")) {
                        detalle.setPathPDF("/home/facturas/" + año + "/" + mes + "/" + dia + "/" + "/" + request.getRemoteUser() + "/" + multipartFile.getOriginalFilename());
                        detalle.setNombrePDF(multipartFile.getOriginalFilename());
                    }
                    if (multipartFile.getOriginalFilename().contains(".xml")) {
                        detalle.setPathXMl("/home/facturas/" + año + "/" + mes + "/" + dia + "/" + request.getRemoteUser() + "/" + multipartFile.getOriginalFilename());
                        detalle.setNombreXMl(multipartFile.getOriginalFilename());
                    }
                }
            }
            ////Subir archivos\\\
        } catch (NullPointerException e) {
            log.warn("no se subieron archivos");
            e.printStackTrace();
        }

        InformeProveedor informe = (InformeProveedor) request.getSession().getAttribute("informeId");
        detalle.setInformeProveedor(informe);
        Usuario usuario = ambiente.obtieneUsuario();
        detalle.setFechaCaptura(new Date());
        detalle.setUsuarioAlta(usuario);
        detalle.setStatus(Constantes.STATUS_ACTIVO);
        if (usuario.isEmpleado()) {
            Empleado empleado = (Empleado) usuario;
            detalle.setNombreProveedor(empleado.getNombre());
            detalle.setEmpleado(empleado);

        }
        if (usuario.isProveedor()) {
            ProveedorFacturas proveedorFacturas = (ProveedorFacturas) usuario;
            detalle.setProveedorFacturas(proveedorFacturas);
            detalle.setNombreProveedor(proveedorFacturas.getNombre());
            detalle.setRFCProveedor(proveedorFacturas.getRfc());
        }
        try {
            manager.graba(detalle, usuario);
            request.getSession().setAttribute("detalleId", detalle.getId());

        } catch (ConstraintViolationException e) {
            log.error("No se pudo crear el detalle", e);
            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_NUEVO;
        }

        redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "detalle.graba.message");
        redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{detalle.getNombreProveedor()});

        return "redirect:" + Constantes.PATH_INFORMEPROVEEDOR_DETALLE_LISTA;
    }

    @Transactional
    @RequestMapping(value = "/crea", method = RequestMethod.POST)
    public String crea(HttpServletRequest request, HttpServletResponse response, @Valid InformeProveedorDetalle detalle,
            BindingResult bindingResult, Errors errors, Model modelo, RedirectAttributes redirectAttributes) throws Exception {
        for (String nombre : request.getParameterMap().keySet()) {
            log.debug("Param: {} : {}", nombre, request.getParameterMap().get(nombre));
        }
        if (bindingResult.hasErrors()) {
            log.debug("Hubo algun error en la forma, regresando");
            Map<String, Object> params = new HashMap<>();
            params.put("empresa", request.getSession()
                    .getAttribute("empresaId"));

            this.despliegaBindingResultErrors(bindingResult);

            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_NUEVA;
        }
        NumberFormat nf_mx = NumberFormat.getNumberInstance(Locale.getDefault());

        log.info("localedefault{}***", Locale.getDefault());
        Map<String, Object> params = new HashMap<>();
        try {
            BigDecimal dcto = detalle.getDctoProntoPago();
            BigDecimal dctoPago = new BigDecimal(nf_mx.parse(dcto.toString()).doubleValue());
            detalle.setDctoProntoPago(dctoPago);
        } catch (ParseException e) {
            log.info("No se pudo convertir el formato");
        }
        try {
            BigDecimal iva = detalle.getIVA();
            BigDecimal IVA = new BigDecimal(nf_mx.parse(iva.toString()).doubleValue());
            detalle.setIVA(IVA);
        } catch (ParseException e) {
            log.info("No se pudo convertir el formato");
        }
        try {
            BigDecimal sub = detalle.getSubtotal();
            BigDecimal subTotal = new BigDecimal(nf_mx.parse(sub.toString()).doubleValue());
            detalle.setSubtotal(subTotal);
        } catch (ParseException e) {
            log.info("No se pudo convertir el formato");
        }
        try {
            BigDecimal total = detalle.getTotal();
            BigDecimal Total = new BigDecimal(nf_mx.parse(total.toString()).doubleValue());
            detalle.setTotal(Total);
        } catch (ParseException e) {
            log.info("No se pudo convertir el formato");
        }
        InformeProveedor informe = (InformeProveedor) request.getSession().getAttribute("informeId");
        detalle.setInformeProveedor(informe);
        Usuario usuario = ambiente.obtieneUsuario();
        detalle.setFechaCaptura(new Date());
        detalle.setUsuarioAlta(usuario);
        detalle.setStatus(Constantes.STATUS_ACTIVO);
        log.info("empresa{}", informe.getEmpresa().getNombre());
        detalle.setEmpresa(informe.getEmpresa());
        if (usuario.isEmpleado()) {
            Empleado empleado = (Empleado) usuario;
            detalle.setNombreProveedor(empleado.getNombre());
            detalle.setEmpleado(empleado);

        }
        if (usuario.isProveedor()) {
            ProveedorFacturas proveedorFacturas = (ProveedorFacturas) usuario;
            detalle.setProveedorFacturas(proveedorFacturas);
            detalle.setNombreProveedor(proveedorFacturas.getNombre());
            detalle.setRFCProveedor(proveedorFacturas.getRfc());
        }
        try {
            manager.graba(detalle, usuario);
            request.getSession().setAttribute("detalleId", detalle.getId());

        } catch (ConstraintViolationException e) {
            log.error("No se pudo crear el detalle", e);
            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_NUEVA;
        } catch (FacturaRepetidaFFacturaException e) {
            errors.rejectValue("folioFactura", "facturaRepetidaException.folioFactura",
                    new String[]{e.getMessage()}, null);
            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_NUEVA;

        }

        Boolean xml = true;
        request.getSession().setAttribute("esXml", xml);
        return "redirect:" + "/factura/informeProveedorDetalle/upload";
    }

    @Transactional
    @RequestMapping(value = {"/upload"}, method = RequestMethod.GET)
    public String fileToUpload(ModelMap model) throws Exception {
        UploadFileForm form = new UploadFileForm();
        model.addAttribute("uploadFileForm", form);
        return "/factura/informeProveedorDetalle/uploadFile";
    }

    @Transactional
    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
    public String uploadFile(HttpServletRequest request, @ModelAttribute("uploadFileForm") UploadFileForm uploadFileForm,
            BindingResult bindingResult, Errors errors, RedirectAttributes redirectAttributes) throws Exception {
        log.info("entrando a uploadFile");
        despliegaBindingResultErrors(bindingResult);
        Long id = (Long) request.getSession().getAttribute("detalleId");
        InformeProveedorDetalle detalle = manager.obtiene(id);
        log.debug("detalle {}", detalle.getId());
        Boolean sw = false;
        Boolean xml = false;
        Usuario usuario = ambiente.obtieneUsuario();
        Calendar calendar = GregorianCalendar.getInstance();
        int año = calendar.get(Calendar.YEAR);
        int mes = calendar.get(Calendar.MONTH);
        int dia = calendar.get(Calendar.DATE);
        String fileName = uploadFileForm.getFile().getOriginalFilename();
        //Subir archivo
        log.debug("file {}", uploadFileForm.getFile().getOriginalFilename());
        log.info("nombre archvo{}", fileName);
        String uploadDir = "/home/facturas/" + año + "/" + mes + "/" + dia + "/" + request.getRemoteUser() + "/" + uploadFileForm.getFile().getOriginalFilename();
        log.debug("upload dir {} ", uploadDir);
        File dirPath = new File(uploadDir);
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }
        String ext = fileName.substring(fileName.lastIndexOf("."));
        log.info("extensión{}" + ext);
        sw = true;
        Boolean esXml = (Boolean) request.getSession().getAttribute("esXml");
        log.debug("Archivo {} subido... ", uploadFileForm.getFile().getOriginalFilename());
        if (fileName.toLowerCase().contains(".xml")) {
            detalle.setPathXMl("/home/facturas/" + año + "/" + mes + "/" + dia + "/" + request.getRemoteUser() + "/" + uploadFileForm.getFile().getOriginalFilename());
            detalle.setNombreXMl(uploadFileForm.getFile().getOriginalFilename());
            detalle.setXmlFile(uploadFileForm.getFile().getBytes());
            manager.actualiza(detalle, usuario);
            xml = true;
            request.getSession().setAttribute("esPdf", xml);
            request.getSession().setAttribute("esXml", false);
            return "redirect:" + "/factura/informeProveedorDetalle/upload";
        }
        if (fileName.toLowerCase().contains(".pdf")) {
            detalle.setPathPDF("/home/facturas/" + año + "/" + mes + "/" + dia + "/" + request.getRemoteUser() + "/" + uploadFileForm.getFile().getOriginalFilename());
            detalle.setNombrePDF(uploadFileForm.getFile().getOriginalFilename());
            detalle.setPdfFile(uploadFileForm.getFile().getBytes());
            manager.actualiza(detalle, usuario);
            request.getSession().setAttribute("esPdf", false);
        }
//            else {
//            // errors.reject("archivo.invalido.facturas" + fileName);
//            if (esXml) {
//                redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "archivoXML.invalido.facturas");
//            } else {
//                redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "archivoPDF.invalido.facturas");
//            }
//            redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{fileName, ext});
//            return "redirect:" + "/factura/informeProveedorDetalle/upload";
//        }
        uploadFileForm.getFile().transferTo(new File(uploadDir));

        return "redirect:" + Constantes.PATH_INFORMEPROVEEDOR_DETALLE_LISTA;
    }

    @Transactional
    @RequestMapping(value = {"/updateXMLFile"}, method = RequestMethod.GET)
    public String updateXMLFile(HttpServletRequest request, ModelMap model) throws Exception {
        UploadFileForm form = new UploadFileForm();
        model.addAttribute("uploadFileForm", form);
        request.getSession().setAttribute("esPdf", false);
        request.getSession().setAttribute("esXml", true);
        return "/factura/informeProveedorDetalle/updateUploadedFile";
    }

    @Transactional
    @RequestMapping(value = {"/updatePDFFile"}, method = RequestMethod.GET)
    public String updatePDFFile(HttpServletRequest request, ModelMap model) throws Exception {
        UploadFileForm form = new UploadFileForm();
        
        model.addAttribute("uploadFileForm", form);
        request.getSession().setAttribute("esPdf", true);
        request.getSession().setAttribute("esXml", false);
        return "/factura/informeProveedorDetalle/updateUploadedFile";
    }

    @Transactional
    @RequestMapping(value = "/updateUploadedFile", method = RequestMethod.POST)
    public String updateUploadedFile(HttpServletRequest request, @ModelAttribute("uploadFileForm") UploadFileForm uploadFileForm,
            BindingResult bindingResult, Errors errors, RedirectAttributes redirectAttributes) throws Exception {
        log.info("entrando a uploadFile");
        despliegaBindingResultErrors(bindingResult);
        Long id = (Long) request.getSession().getAttribute("detalleUpdateFileId");
        InformeProveedorDetalle detalle = manager.obtiene(id);
        log.debug("detalle {}", detalle.getId());
        Usuario usuario = ambiente.obtieneUsuario();
        Calendar calendar = GregorianCalendar.getInstance();
        int año = calendar.get(Calendar.YEAR);
        int mes = calendar.get(Calendar.MONTH);
        int dia = calendar.get(Calendar.DATE);
        String fileName = uploadFileForm.getFile().getOriginalFilename();
        log.info("nombre archvo{}", fileName);
        String uploadDir = "/home/facturas/" + año + "/" + mes + "/" + dia + "/" + request.getRemoteUser() + "/" + uploadFileForm.getFile().getOriginalFilename();
        log.debug("upload dir {} ", uploadDir);
        File dirPath = new File(uploadDir);
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }
        if (fileName.toLowerCase().contains(".xml")) {
            detalle.setPathXMl("/home/facturas/" + año + "/" + mes + "/" + dia + "/" + request.getRemoteUser() + "/" + uploadFileForm.getFile().getOriginalFilename());
            detalle.setNombreXMl(uploadFileForm.getFile().getOriginalFilename());
            detalle.setXmlFile(uploadFileForm.getFile().getBytes());
            manager.actualiza(detalle, usuario);
            request.getSession().setAttribute("esPdf", false);
            request.getSession().setAttribute("esXml", false);
        }
        if (fileName.toLowerCase().contains(".pdf")) {
            detalle.setPathPDF("/home/facturas/" + año + "/" + mes + "/" + dia + "/" + request.getRemoteUser() + "/" + uploadFileForm.getFile().getOriginalFilename());
            detalle.setNombrePDF(uploadFileForm.getFile().getOriginalFilename());
            detalle.setPdfFile(uploadFileForm.getFile().getBytes());
            manager.actualiza(detalle, usuario);
            request.getSession().setAttribute("esPdf", false);
            request.getSession().setAttribute("esXml", false);
        }
        uploadFileForm.getFile().transferTo(new File(uploadDir));

        return "redirect:" + Constantes.PATH_INFORMEPROVEEDOR_DETALLE_LISTA;
    }

    @Transactional
    @RequestMapping(value = "/actualiza", method = RequestMethod.POST)
    public String actualiza(HttpServletRequest request, HttpServletResponse response, @Valid InformeProveedorDetalle detalle,
            BindingResult bindingResult, Errors errors, Model modelo, RedirectAttributes redirectAttributes,
            @ModelAttribute("uploadForm") FileUploadForm uploadForm) throws Exception {
        for (String nombre : request.getParameterMap().keySet()) {
            log.debug("Param: {} : {}", nombre, request.getParameterMap().get(nombre));
        }
        utils.despliegaBindingResultErrors(bindingResult);
        if (bindingResult.hasErrors()) {
            log.debug("Hubo algun error en la forma, regresando");
            Map<String, Object> params = new HashMap<>();
            params.put("empresa", request.getSession()
                    .getAttribute("empresaId"));

            this.despliegaBindingResultErrors(bindingResult);

            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_NUEVO;
        }
        Usuario usuario = ambiente.obtieneUsuario();
        detalle.setFechaModificacion(new Date());
        detalle.setUsuarioMOdificacion(usuario);

        try {
            InformeProveedor informe = (InformeProveedor) request.getSession().getAttribute("informeId");
            detalle.setInformeProveedor(informe);
            InformeProveedorDetalle detalleTmp = manager.obtiene(detalle.getId());
            detalle.setNombrePDF(detalleTmp.getNombrePDF());
            detalle.setNombreXMl(detalleTmp.getNombreXMl());
            detalle.setPathPDF(detalleTmp.getPathPDF());
            detalle.setPathXMl(detalleTmp.getPathXMl());
            log.debug("Paquete {}", detalle);
            manager.actualiza(detalle, usuario);
        } catch (ConstraintViolationException e) {
            log.error("No se pudo crear el detalle", e);
            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_NUEVO;
        }

        redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "detalle.graba.message");
        redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{detalle.getNombreProveedor()});

        return "redirect:" + Constantes.PATH_INFORMEPROVEEDOR_DETALLE_LISTA;
    }

    @RequestMapping("/edita/{id}")
    public String edita(@PathVariable Long id, Model modelo) {
        log.debug("Editar cuenta de detalles{}", id);
        Map<String, Object> params = new HashMap<>();
        params = managerInforme.lista(params);
        List<InformeProveedor> informes = (List) params.get(Constantes.CONTAINSKEY_INFORMESPROVEEDOR);
        modelo.addAttribute(Constantes.CONTAINSKEY_INFORMESPROVEEDOR, informes);
        InformeProveedorDetalle detalle = manager.obtiene(id);
        modelo.addAttribute(Constantes.ADDATTRIBUTE_INFORMEPROVEEDOR_DETALLE, detalle);
        return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_EDITA;
    }

    @Transactional
    @RequestMapping(value = "/elimina", method = RequestMethod.POST)
    public String elimina(HttpServletRequest request, @RequestParam Long id, Model modelo,
            @ModelAttribute InformeProveedorDetalle detalle, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        log.debug("Elimina cuenta de detalles");
        try {
            String nombre = manager.elimina(id);

            redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "detalle.elimina.message");
            redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{nombre});
        } catch (Exception e) {
            log.error("No se pudo eliminar el tipo de paquete " + id, e);
            bindingResult.addError(new ObjectError(Constantes.ADDATTRIBUTE_INFORMEPROVEEDOR_DETALLE, new String[]{"detalle.no.elimina.message"}, null, null));
            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_VER;
        }

        return "redirect:" + Constantes.PATH_INFORMEPROVEEDOR_DETALLE_LISTA;
    }

    @Transactional
    @RequestMapping(value = "/eliminaValida", method = RequestMethod.POST)
    public String eliminaValida(HttpServletRequest request, @RequestParam Long id, Model modelo,
            @ModelAttribute InformeProveedorDetalle detalle, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        log.debug("Elimina cuenta de detalles");
        try {
            manager.elimina(id);

            redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "detalle.elimina.message");
            redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{detalle.getNombreProveedor()});
        } catch (Exception e) {
            log.error("No se pudo eliminar el tipo de paquete " + id, e);
            bindingResult.addError(new ObjectError(Constantes.ADDATTRIBUTE_INFORMEPROVEEDOR_DETALLE, new String[]{"detalle.no.elimina.message"}, null, null));
            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_VER;
        }
        return "redirect:/factura/informeProveedorDetalle/revisar";
    }

    @RequestMapping(value = "/descargarPdf/{id}", method = RequestMethod.GET)
    public ModelAndView handleRequestPDF(@PathVariable Long id, Model modelo, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        InformeProveedorDetalle detalle = manager.obtiene(id);
        try {
            String nombreFichero = detalle.getNombrePDF();
            String unPath = detalle.getPathPDF();

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + nombreFichero + "\"");

            InputStream is = new FileInputStream(unPath);

            IOUtils.copy(is, response.getOutputStream());

            response.flushBuffer();

        } catch (IOException ex) {
            throw ex;
        }
        return null;
    }

    @RequestMapping(value = "/descargarXML/{id}", method = RequestMethod.GET)
    public ModelAndView handleRequestXML(@PathVariable Long id, Model modelo, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        InformeProveedorDetalle detalle = manager.obtiene(id);
        try {
            String nombreFichero = detalle.getNombreXMl();
            String unPath = detalle.getPathXMl();

            response.setContentType("application/xml");
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + nombreFichero + "\"");

            InputStream is = new FileInputStream(unPath);

            IOUtils.copy(is, response.getOutputStream());

            response.flushBuffer();

        } catch (IOException ex) {
            throw ex;
        }
        return null;
    }

    @RequestMapping("/downloadPdfFile/{id}")
    public String downloadPdfBD(@PathVariable("id") Long id, HttpServletResponse response) {

        InformeProveedorDetalle doc = manager.obtiene(id);
        try {
            OutputStream out = response.getOutputStream();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + doc.getNombrePDF() + "\"");
            FileCopyUtils.copy(doc.getPdfFile(), out);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @RequestMapping("/downloadXmlFile/{id}")
    public String downloadXmlBD(@PathVariable("id") Long id, HttpServletResponse response) {

        InformeProveedorDetalle doc = manager.obtiene(id);
        try {
            OutputStream out = response.getOutputStream();
            response.setContentType("application/xml");
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + doc.getNombreXMl() + "\"");
            FileCopyUtils.copy(doc.getXmlFile(), out);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Transactional
    @RequestMapping(value = "/autorizar", method = RequestMethod.GET)
    public String autorizar(HttpServletRequest request, Model modelo, RedirectAttributes redirectAttributes) throws Exception {
        log.debug("Entrando a Autorizar informe");
        String checks = request.getParameter("checkFacturasid");

        log.debug("map {}", request.getParameterMap().toString());
        log.debug("names {}", request.getParameterNames().toString());
        Map<String, String[]> parameterMap = request.getParameterMap();
        parameterMap.get("key");
        Boolean autorizar = false;
        Boolean rechazar = false;
        ArrayList ids = new ArrayList();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String nombre = parameterNames.nextElement();
            if (nombre.startsWith("botonAut")) {
                autorizar = true;
            }
            if (nombre.startsWith("botonRe")) {
                rechazar = true;
            }

            if (nombre.startsWith("checkFac")) {
                String[] id = nombre.split("-");
                log.debug("id ={}", id[1]);
                ids.add(id[1]);
            }
        }
        Usuario usuario = ambiente.obtieneUsuario();
        if (autorizar) {
            log.debug("enviando al metodo para autorizar");
            try {
                List<InformeProveedorDetalle> detalles = manager.autorizar(ids, usuario);

            } catch (ClabeNoCoincideException e) {
                log.debug("la clabe de la factura con id= {} no coincide", e);
                if (e != null) {
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "clabe.no.coincide");
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{e.getMessage()});
                }
                return "redirect:/factura/informeProveedorDetalle/revisar";
            } catch (ProveedorNoCoincideException e) {
                log.debug("el proveedor de la factura con id= {} no coincide", e);
                if (e != null) {
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "proveedor.no.coincide");
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{e.getMessage()});
                }
                return "redirect:/factura/informeProveedorDetalle/revisar";
            } catch (BancoNoCoincideException e) {
                log.debug("el banco de la factura con id= {} no coincide", e);
                if (e != null) {
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "banco.no.coincide");
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{e.getMessage()});
                }
                return "redirect:/factura/informeProveedorDetalle/revisar";
            } catch (CuentaChequeNoCoincideException e) {
                log.debug("la cuenta de la factura con id= {} no coincide", e);
                if (e != null) {
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "cuenta.no.coincide");
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{e.getMessage()});
                }
                return "redirect:/factura/informeProveedorDetalle/revisar";

            } catch (FormaPagoNoCoincideException e) {
                log.debug("la forma de pago de la factura con id= {} no coincide", e);
                if (e != null) {
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "cuenta.no.coincide");
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{e.getMessage()});
                }
                return "redirect:/factura/informeProveedorDetalle/revisar";
            }

        }
        if (rechazar) {
            log.debug("enviando al metodo para rechazar");
            try {
                manager.rechazar(ids, usuario);
            } catch (ProveedorNoCoincideException e) {
                log.debug("el banco de la factura con id= {} no coincide", e);
                if (e != null) {
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "banco.no.coincide");
                    redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{e.getMessage()});
                }
                return "redirect:/factura/informeProveedorDetalle/revisar";
            }
        }

        redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "informeProveedor.finaliza.message");

        ProveedorFacturas proveedorFacturas = (ProveedorFacturas) request.getSession().getAttribute("proveedorFacturas");
        modelo.addAttribute("proveedorFacturas", proveedorFacturas);
        InformeProveedorDetalle detalle = new InformeProveedorDetalle();
        modelo.addAttribute(Constantes.ADDATTRIBUTE_INFORMEPROVEEDOR_DETALLE, detalle);

        return "redirect:/factura/informeProveedorDetalle/revisar";
    }

    @Transactional
    @RequestMapping(value = "/pagar", method = RequestMethod.GET)
    public String pagar(HttpServletRequest request, Model modelo, RedirectAttributes redirectAttributes) throws Exception {
        log.debug("Entrando a Autorizar informe");
        String checks = request.getParameter("checkFacturasid");

        log.debug("map {}", request.getParameterMap().toString());
        log.debug("names {}", request.getParameterNames().toString());
        Map<String, String[]> parameterMap = request.getParameterMap();
        parameterMap.get("key");
        List<Long> ids = new ArrayList();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String nombre = parameterNames.nextElement();

            if (nombre.startsWith("checkFac")) {
                String[] id = nombre.split("-");
                log.debug("id ={}", id[1]);
                ids.add(Long.parseLong(id[1]));
            }
        }
        Usuario usuario = ambiente.obtieneUsuario();
        List<InformeProveedorDetalle> detalles = manager.obtiene(ids);

        request.getSession().setAttribute("facturasPagar", detalles);
        Contrarecibo contrarecibo = new Contrarecibo();
        modelo.addAttribute(Constantes.ADDATTRIBUTE_CONTRARECIBO, contrarecibo);
        return "/factura/informeProveedorDetalle/fecha";
    }

    @RequestMapping("/fecha")
    public String asignarFecha(HttpServletRequest request, Model modelo) {
        log.info("nuevo contrarecibo");
        Contrarecibo contrarecibo = new Contrarecibo();
        modelo.addAttribute(Constantes.ADDATTRIBUTE_CONTRARECIBO, contrarecibo);
        modelo.addAttribute(Constantes.ADDATTRIBUTE_CONTRARECIBO, contrarecibo);
        return "/factura/informeProveedorDetalle/fecha";
    }

    @RequestMapping("/cambiarFecha/{id}")
    public String cambiarFecha(@PathVariable Long id, HttpServletRequest request, Model modelo) {
        log.info("cambiando fecha a contrarecivo");

        Contrarecibo contrarecibo = contrareciboManager.obtiene(id);
        modelo.addAttribute(Constantes.ADDATTRIBUTE_CONTRARECIBO, contrarecibo);
        request.getSession().setAttribute("contrareciboFecha", contrarecibo);
        return "/factura/informeProveedorDetalle/fecha";
    }

    @Transactional
    @RequestMapping(value = "/actualizaFecha", method = RequestMethod.POST)
    public String actualizaFecha(HttpServletRequest request, HttpServletResponse response, @Valid Contrarecibo contrarecibo,
            BindingResult bindingResult, Errors errors, Model modelo, RedirectAttributes redirectAttributes) throws Exception {
        for (String nombre : request.getParameterMap().keySet()) {
            log.debug("Param: {} : {}", nombre, request.getParameterMap().get(nombre));
        }
        utils.despliegaBindingResultErrors(bindingResult);
        if (bindingResult.hasErrors()) {
            log.debug("Hubo algun error en la forma, regresando");
            Map<String, Object> params = new HashMap<>();
            params.put("empresa", request.getSession()
                    .getAttribute("empresaId"));

            this.despliegaBindingResultErrors(bindingResult);

            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_NUEVO;
        }

        try {

            Usuario usuario = ambiente.obtieneUsuario();
            if (contrarecibo.getId() == null) {
                List<InformeProveedorDetalle> detalles = (List<InformeProveedorDetalle>) request.getSession().getAttribute("facturasPagar");
                contrarecibo.setStatus(Constantes.STATUS_ACTIVO);
                ProveedorFacturas proveedorFacturas = detalles.get(0).getInformeProveedor().getProveedorFacturas();
                contrarecibo.setProveedorFacturas(proveedorFacturas);
                contrarecibo.setUsuarioAlta(usuario);
                contrareciboManager.graba(contrarecibo, usuario);
                for (InformeProveedorDetalle x : detalles) {

                    x.setContrarecibo(contrarecibo);
                    x.setStatus(Constantes.STATUS_AUTORIZADO);
                    manager.actualiza(x, usuario);
                }
            } else {
                log.info("actualizando");
                log.info("contrarecibo a actualizar{}", contrarecibo.toString());
                Contrarecibo cont = contrareciboManager.obtiene(contrarecibo.getId());
                cont.setFechaPago(contrarecibo.getFechaPago());
                contrareciboManager.actualiza(cont, usuario);
            }

        } catch (ConstraintViolationException e) {
            log.error("No se pudo crear el detalle", e);
            return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_NUEVO;
        }

        redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "detalle.graba.message");
        redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE_ATTRS, new String[]{contrarecibo.getFechaPago().toString()});

        return "redirect:" + Constantes.PATH_INFORMEPROVEEDOR_DETALLE_CONTRARECIBOS;
    }

    @RequestMapping("/reporteContrarecibo/{id}")
    public String reporteContrarecibo(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response, Model modelo) throws
            ReporteException {
        log.debug("Nuevo paquete");
        Map<String, Object> params = new HashMap<>();
        Long empresaId = (Long) request.getSession().getAttribute("empresaId");
        params.put("empresa", empresaId);
        Contrarecibo contrarecibo = contrareciboManager.obtiene(id);
        List<ContrareciboVO> vos = contrareciboManager.ListadeContrarecibosVO(id);
//        params.put(Constantes.CONTAINSKEY_CONTRARECIBOS, contrarecibo);
        log.debug("Entrando a tipo");
        params.put("reporte", true);
        try {
            params.put(Constantes.CONTAINSKEY_CONTRARECIBOS, vos);
//            params = contrareciboManager.lista(params);
        } catch (Exception ex) {
            log.error("Error al intentar obtener el censo de colportores");
        }
        log.debug("listaContrarecibos**-{}", (List<ContrareciboVO>) params.get(Constantes.CONTAINSKEY_CONTRARECIBOS));
        log.debug("empresa**-{}", ambiente.obtieneUsuario().getEmpresa().getId());
        log.debug("Obtuvo listado");
        try {
            log.debug("Generando reporte");
            generaReporte("PDF", (List<ContrareciboVO>) params.get(Constantes.CONTAINSKEY_CONTRARECIBOS), response, request,
                    "contrareciboFacturas", Constantes.EMP, ambiente.obtieneUsuario().getEmpresa().getId());
            log.debug("Genero reporte");
            return null;
        } catch (Exception e) {
            log.error("No se pudo generar el reporte", e);
            e.printStackTrace();
        }

        return Constantes.PATH_INFORMEPROVEEDOR_DETALLE_CONTRARECIBOS;
    }

    @RequestMapping("/eliminaContrarecibo/{id}")
    public String eliminaContrarecibo(@PathVariable Long id, Model modelo) {
        log.debug("Mostrando paquete {}", id);
        Usuario usuario = ambiente.obtieneUsuario();
        Contrarecibo contrarecibo = contrareciboManager.obtiene(id);
        contrarecibo.setFechaModificacion(new Date());
        contrarecibo.setUsuarioModificacion(usuario);
        String folio = contrareciboManager.elimina(id);

        return "redirect:" + Constantes.PATH_INFORMEPROVEEDOR_DETALLE_CONTRARECIBOS;
    }

    @RequestMapping("/asignarFacturas")
    public String asignarArchivos(RedirectAttributes redirectAttributes) throws FileNotFoundException, IOException {
        Usuario usuario = ambiente.obtieneUsuario();
        String path = "/home/facturas/2014/06/19/administracion@lacarlota.um.edu.mx";
        File dir = new File(path);
        String[] ficheros = dir.list();
        for (String nombreArchivo : ficheros) {
            InformeProveedorDetalle detalle = manager.obtiene(nombreArchivo);
            Path hubicacion = Paths.get(path + nombreArchivo);
            byte[] bytes = Files.readAllBytes(hubicacion);
            if (nombreArchivo.contains(".pdf")) {
                detalle.setPdfFile(bytes);
            }
            if (nombreArchivo.contains(".xml")) {
                detalle.setXmlFile(bytes);
            }
            manager.actualiza(detalle, usuario);
        }
        redirectAttributes.addFlashAttribute(Constantes.CONTAINSKEY_MESSAGE, "detalle.graba.message");
//        return "redirect:" + Constantes.PATH_INFORMEPROVEEDOR_DETALLE_CONTRARECIBOS;
        return "factura/index";
    }

}
