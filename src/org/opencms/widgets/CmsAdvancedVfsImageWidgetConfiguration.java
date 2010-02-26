/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/widgets/Attic/CmsAdvancedVfsImageWidgetConfiguration.java,v $
 * Date   : $Date: 2010/02/26 10:38:31 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.widgets;

import org.opencms.file.CmsObject;
import org.opencms.json.JSONObject;

/**
 * Configuration options for the advanced vfs image gallery widget (e.g. AdvancedVfsImageWidget).<p>
 * 
 * The configuration options are read from the configuration String of the widget. 
 * For nested XML schemas the configuration String must be defined inside the nested content.<p>
 * 
 * The configuration String has to be formatted as JSON object with following structure:
 * <code>{configuration}</code>
 * 
 * Following keys can be used for all gallery types("gallerykey") inside the "configuration" of the widget:
 * <ul>
 * <li><code>class</code>: optional class implementing {@link I_CmsGalleryWidgetDynamicConfiguration} to dynamically
 *            configure startup parameters and format values.</li>
 * <li><code>startup</code>: the startup folder('/demo_t3/documents/') or the startup folders(['/demo_t3/documents/','/demo_t3/test/']), can be dynamically generated by the provided class,
 *            in that case, use 'dynamic' as value.</li>
 * <li><code>type</code>: the startup folder type, can be 'gallery' or 'category'. Can be dynamically generated
 *            by the provided class, in that case, use 'dynamic' as value.</li>
 * </ul>
 * 
 * The following additional keys are possible for "imagegallery":
 * <ul>
 * <li><code>class</code>: optional class implementing {@link I_CmsImageWidgetDynamicConfiguration} to dynamically
 *            configure startup parameters and format values.</li>
 * <li><code>formatnames</code>: list of format names to select, with pairs of selectable value and selectable text,
 *            e.g. value1:optiontext1|value2:optiontext2</li>
 * <li><code>formatvalues</code>: corresponding format values to the format names list,
 *            can be dynamically generated by the dynamic configuration class.
 *            The list of values should contain width and height information, with a '?' as sign for dynamic size
 *            and with an 'x' as separator for width and height.
 *            Example: ['200x?', '800x600']</li>
 * <li><code>scaleparams</code>: default scale parameters (no width, height or crop information should be provided!)</li>
 * <li><code>usedescription</code>: indicates if the description input field for the image should be shown or not.</li>
 * <li><code>useformat</code>: indicates if the format select box for the image should be shown or not.</li>
 * </ul>
 * 
 * Example configurations can look like this:<p>
 * <code>{startup:['species/composite-plants/','species/'],type:'category', useformat:false}</code><p>
 * <code>{startup:/demo_t3/images/,type:'gallery', useformat:true}</code><p>
 *
 * @author Polina Smagina
 * 
 * @version $Revision: 1.1 $ 
 * 
 * @since  
 */
public class CmsAdvancedVfsImageWidgetConfiguration extends CmsAdvancedGalleryWidgetConfiguration {

    /**
     * Generates an initialized configuration for the advanced gallery item widget using the given configuration string.<p>
     * 
     * @param cms an initialized instance of a CmsObject
     * @param widgetDialog the dialog where the widget is used on
     * @param param the widget parameter to generate the widget for
     * @param configuration the widget configuration string
     */
    public CmsAdvancedVfsImageWidgetConfiguration(
        CmsObject cms,
        I_CmsWidgetDialog widgetDialog,
        I_CmsWidgetParameter param,
        String configuration) {

        super();
        init(cms, widgetDialog, param, configuration);

    }

    /** The flag if the description field should be shown. */
    protected boolean m_showDescription;

    /**
     * Returns if the description field should be shown.<p>
     * 
     * @return true if the description field should be shown, otherwise false
     */
    public boolean isShowDescription() {

        return m_showDescription;
    }

    /**
     * Sets if the description field should be shown.<p>
     * 
     * @param showDescription true if the description field should be shown, otherwise false
     */
    private void setShowDescription(boolean showDescription) {

        m_showDescription = showDescription;
    }

    /**
     * Returns the gallery type name from the configuration. <p>
     * 
     * @param jsonObj the configuration of the gallery as json object 
     * @return the gallery name key name
     */
    @Override
    protected CmsGalleryConfigKeys getGalleryType(JSONObject jsonObj) {

        return CmsGalleryConfigKeys.imagegallery;

    }

    /**
     * Sets the common parameter for the gallery configuration. <p>
     * 
     * @param cms an initialized instance of a CmsObject
     * @param widgetDialog the dialog where the widget is used on
     * @param param the widget parameter to generate the widget for
     * @param jsonObj configuration as json obj
     */
    @Override
    protected void prepareAndSetImageGalleryConfigParams(
        CmsObject cms,
        I_CmsWidgetDialog widgetDialog,
        I_CmsWidgetParameter param,
        JSONObject jsonObj) {

        if (jsonObj == null) {
            // no configuration
            return;
        }
        setImageGalleryConfigParams(cms, widgetDialog, param, jsonObj);

        // determine if the description field should be shown
        setShowDescription(jsonObj.optBoolean(CmsGalleryConfigParam.CONFIG_KEY_USEDESCRIPTION.getName()));
    }

}
