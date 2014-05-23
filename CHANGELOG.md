Change Log
===============================================================================
Version 1.3.2 *(2014-05-23)*
----------------------------
* Fixed: Editing account modifies the transaction type of transfer transactions
* Fixed: Bug causing double entry transactions not to be considered equal
* Fixed: Computation of account balance did not properly consider double entries
* Improved: Double-entry accounting is now activated by default
* Improved: Reliability of account structure import
* Improved: Restricted parent/child account hierarchies relationships to those defined by GnuCash accounting
* Improved: Dutch translation
* Improved: German translation

Version 1.3.1 *(2014-02-14)*
----------------------------
* Fixed: Crash when bulk moving transactions
* Fixed: Missing string for internationalization in ru_RU locale
* Fixed: Random crashes when opening ScheduledTransactions list
* Fixed: Blank screen after closing AccountFormFragment
* Fixed: Correct normal balance of the different types of ASSET accounts
* Fixed: Limit the target accounts for bulk transfers to same currency and non-placeholder accounts
* Improved: Remember last opened tab in accounts list
* Improved: Added version information for feedback email
* Improved: Lists of accounts are now sorted by the fully qualified account name

Version 1.3.0 *(2014-02-10)*
----------------------------
* Fixed: Some file managers do not display all files available for import
* Fixed: Crash when deleting account from accounts list
* Fixed: CASH accounts should have normal DEBIT balance
* Fixed: Crash when quickly opening and navigating from transactions list
* Feature: Mark favorite accounts and quickly access them
* Feature: Display different tabs for recent, favorite and all accounts
* Feature: Add, view and delete recurring transactions (daily, weekly, monthly)
* Feature: Mark accounts as placeholder accounts (cannot contain transactions)
* Feature: Set a default transfer account for each account
* Feature: Color code accounts & themed account views
* Feature: Create default GnuCash account structure from within app
* Improved: Enabled one-button click for rating app and sending feedback
* Improved: Clicking on version information now shows changelog
* Improved: Delete account and all its sub-accounts
* Improved: Sub-accounts default to same account type as parent account
* Improved: Use tab views for sub-accounts and transactions inside accounts

Version 1.2.7 *(2013-12-18)*
----------------------------
* Fixed: Export format always defaults to QIF, ignoring user preference
* Improved: Better responsiveness of add transaction and add account buttons
* Improved: Russian translation

Version 1.2.6 *(2013-12-06)*
----------------------------
* Feature: Support for QIF export format
* Improved: CREDIT/DEBIT meaning now matches GnuCash desktop. Effect on account balance depends on type of account

Version 1.2.5 *(2013-09-17)*
----------------------------
* Feature: Search accounts by name
* Fixed: crash when deleting accounts
* Fixed: auto-completing transaction names does not copy the time or export flag
* Fixed: random crash when opening app (or loading accounts)

Version 1.2.4 *(2013-09-05)*
----------------------------
* Added support for detecting placeholder accounts during import
* Use full qualified account names in account selection spinners
* Loads complete transaction as a template when the autocomplete suggestion is selected
* Fixed: selecting items from lists caused multiple to be selected in the wrong positions
* Fixed: widgets not updated when all accounts or all transactions are deleted
* Other minor bug fixes.

Version 1.2.3 *(2013-08-28)*
----------------------------
* Fixed: crashes when editing/creating transactions
* Feature: Added Chinese language translation
* Feature: Autocomplete transaction descriptions
* Improved reliability of importing stock accounts
* Improved speed of loading account balance
* Improved increased touch target area of "new transaction" button in accounts list view

Version 1.2.2 *(2013-06-23)*
----------------------------
* Fixed: bug with importing accounts
* Fixed: deleting an account renders sub-accounts inaccessible.
* Fixed: impossible to scroll new accounts screen
* Updated Brazilian Portuguese translations

Version 1.2.1 *(2013-06-22)*
----------------------------
* Fixed: crash when opening Settings on devices with Gingerbread or earlier
* Improved performance for loading list of accounts and transactions
* Show progress dialog for importing accounts and improve reliability

Version 1.2.0 *(2013-06-20)*
----------------------------
* Feature: Import GnuCash desktop account structure
* Feature: Nested display of account hierarchy
* Feature: Options for deleting all accounts/transactions
* Feature: Preliminary support for account types
* Fixed:   Account balance now takes sub-accounts into consideration
* Fixed:   Support for GnuCash ROOT account (will not be displayed)

Version 1.1.2 *(2013-02-03)*
----------------------------
* Fixed: Crash upon screen rotation when creating account
* Fixed: Crash when entering a transaction after deleting an account which has a widget

Version 1.1.1 *(2013-01-31)*
----------------------------
* Updated German and Norwegian (Bokmal) translations

Version 1.1.0 *(2013-01-31)*
----------------------------

* Feature: Double entry accounting - every transaction is a transfer
* Feature: Nested accounts
* Feature: SGML support for OFX exports. Exports are now SGML by default 
* Feature: Display what's new to user after minor/major updates
* Improved: Reworked UX for creating accounts
* Improved: Default accounts now match GnuCash desktop accounts
* Fixed: Crash when creating accounts with special characters in the names
* Fixed: GnuCash declares itself as launcher application
* Fixed: Encoding of exported OFX not properly detected by GnuCash desktop

Version 1.0.3 *(2012-11-23)*
----------------------------

* Fixed: Crash when determining currency for en_UK which is not an ISO 3611 country
* Fixed: Crashes on Android 4.2 when editing transactions
* Improved: Better handle rotation when creating transactions
* Improved: Spanish & Italian translations

Version 1.0.2 *(2012-11-09)*
----------------------------

* Fixed: Default transaction type setting not working for non-English locales
* Added new default settings for exporting
* Improved French and German translations


Version 1.0.1 *(2012-11-05)*
----------------------------

* Feature: Select default transaction type from settings
* Feature: Navigate account transactions using action bar navigation lists
* Feature: Brazilian Portuguese translation now available
* Fixed:   Inconsistencies in some translation strings 


Version 1.0.0 *(2012-11-01)*
----------------------------
Initial release.
