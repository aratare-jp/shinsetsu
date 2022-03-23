(ns shinsetsu.ui.elastic
  (:require
    [com.fulcrologic.fulcro.algorithms.react-interop :as ri]
    ["@elastic/eui" :refer [EuiProvider
                            EuiButton
                            EuiForm
                            EuiFormRow
                            EuiFieldText
                            EuiSwitch
                            EuiFlexGroup
                            EuiFlexItem
                            EuiPageTemplate
                            EuiEmptyPrompt
                            EuiSpacer
                            EuiPage
                            EuiPageHeader
                            EuiPageBody
                            EuiPageContent
                            EuiPageContentBody
                            EuiLoadingSpinner]]))

(def provider (ri/react-factory EuiProvider))
(def button (ri/react-factory EuiButton))
(def form (ri/react-factory EuiForm))
(def form-row (ri/react-factory EuiFormRow))
(def field-text (ri/react-factory EuiFieldText))
(def switch (ri/react-factory EuiSwitch))
(def flex-group (ri/react-factory EuiFlexGroup))
(def flex-item (ri/react-factory EuiFlexItem))
(def page-template (ri/react-factory EuiPageTemplate))
(def empty-prompt (ri/react-factory EuiEmptyPrompt))
(def spacer (ri/react-factory EuiSpacer))
(def page (ri/react-factory EuiPage))
(def page-header (ri/react-factory EuiPageHeader))
(def page-body (ri/react-factory EuiPageBody))
(def page-content (ri/react-factory EuiPageContent))
(def page-content-body (ri/react-factory EuiPageContentBody))
(def loading-spinner (ri/react-factory EuiLoadingSpinner))
