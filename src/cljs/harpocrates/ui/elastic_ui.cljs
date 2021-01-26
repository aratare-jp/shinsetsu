(ns harpocrates.ui.elastic-ui
  (:require ["@elastic/eui" :refer [EuiButton
                                    EuiCheckboxGroup
                                    EuiFieldText
                                    EuiForm
                                    EuiFormRow
                                    EuiFilePicker
                                    EuiLink
                                    EuiRange
                                    EuiSelect
                                    EuiSpacer
                                    EuiSwitch
                                    EuiText
                                    EuiPage
                                    EuiPageBody
                                    EuiPageContent
                                    EuiPageContentBody
                                    EuiPageContentHeader
                                    EuiPageContentHeaderSection
                                    EuiTitle
                                    EuiControlBar]]
            [com.fulcrologic.fulcro.algorithms.react-interop :as interop]))

(def ui-button (interop/react-factory EuiButton))
(def ui-checkbox-group (interop/react-factory EuiCheckboxGroup))
(def ui-field-text (interop/react-factory EuiFieldText))
(def ui-form (interop/react-factory EuiForm))
(def ui-form-row (interop/react-factory EuiFormRow))
(def ui-file-picker (interop/react-factory EuiFilePicker))
(def ui-link (interop/react-factory EuiLink))
(def ui-range (interop/react-factory EuiRange))
(def ui-select (interop/react-factory EuiSelect))
(def ui-spacer (interop/react-factory EuiSpacer))
(def ui-switch (interop/react-factory EuiSwitch))
(def ui-page (interop/react-factory EuiPage))
(def ui-page-body (interop/react-factory EuiPageBody))
(def ui-page-content (interop/react-factory EuiPageContent))
(def ui-page-content-body (interop/react-factory EuiPageContentBody))
(def ui-page-content-header (interop/react-factory EuiPageContentHeader))
(def ui-page-content-header-section (interop/react-factory EuiPageContentHeaderSection))
(def ui-title (interop/react-factory EuiTitle))
(def ui-control-bar (interop/react-factory EuiControlBar))