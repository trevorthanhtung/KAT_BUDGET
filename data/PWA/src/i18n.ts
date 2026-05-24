import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

import viTranslations from './locales/vi.json'
import enTranslations from './locales/en.json'

i18n
  .use(initReactI18next)
  .init({
    resources: {
      vi: { translation: viTranslations },
      en: { translation: enTranslations },
    },
    lng: 'vi', // default language
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false, // react already safes from xss
    },
  })

export default i18n
