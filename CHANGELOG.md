Change Log
===============================================================================
Version 1.5.5 *(2015-02-20)*
----------------------------
Fixed: QIF not exported when using single-entry transactions
Fixed: Passcode screen can be by-passed using SwipeBack
Fixed: Crash when changing the account name

Version 1.5.4 *(2015-02-16)*
----------------------------
* Fixed: Crash when creating TRADING accounts
* Fixed: Crash when deleting scheduled transactions
* Fixed: Account parent can be set to self, creating a cyclic hierarchy
* Fixed: Transactions not saved when double-entry is enabled but no transfer account is specified
* Improved: Auto-select the device locale currency in the account-creation dialog
* Improved: Upgraded structure of repository to match latest Android conventions
* Improved: Updated instrumentation tests and enabled better test reporting with Spoon

Version 1.5.3 *(2015-02-02)*
----------------------------
* Fixed: Unable to edit double-entry transactions
* Fixed: Edited transactions not flagged unexported
* Fixed: Random crashes when editing transaction splits
* Improved: Long press on transactions triggers context menu

Version 1.5.2 *(2015-01-26)*
----------------------------
* Fixed: Crash when importing XML with TRADING accounts
* Fixed: Full name not updated when account name is changed
* Fixed: Toggle button shown when double-entry is disabled
* Fixed: Amount input problems on some devices or keyboards
* Fixed: Crash when changing the parent of an account
* Fixed: Deleting a transaction only deletes some splits, not the whole.

Version 1.5.1 *(2014-10-08)*
----------------------------
* Fixed: Crash when upgrading from v1.4.x to v1.5.x

Version 1.5.0 *(2014-10-01)*
----------------------------
* Need for speed! Lots of performance optimizations in the application
  - Application balances are now computed faster
  - App loads faster and is more responsive
  - Faster recording of opening balances before delete operations
  - Import and export operations rewritten to perform faster and use less resources
* Fixed: Crash after saving opening balances and trying to create new transactions
* Fixed: Parent account title color sometimes not propagated to child accounts
* Fixed: Recurring transactions scheduled but not saved to database during import
* Fixed: Crash caused by null exception message during import
* Fixed: Poor word-wrap of transaction type labels
* Fixed: Amount values not always displaying the correct sign
* Feature: Select default currency upon first run of application
* Feature: Creating account hierarchy uses the user currency preference
* Feature: Support for reading and writing compressed GnuCash XML files.
* Feature: Set a passcode lock to restrict access to the application
* Feature: Export a QIF file for transactions of each currency in use  
* Improved: Increased stability of import/export operations
* Improved: Exclude multi-currency transactions from QIF exports
* Improved: Display warnings/limitations of different export formats in the export dialog
* Improved: Preserve split memos in QIF export (as much as possible)
* Improved: Child accounts now assigned to account parent upon deletion of account
* Improved: Descendant accounts cannot be selected as a parent account (no cyclic dependencies)

Version 1.4.3 *(2014-09-09)*
----------------------------
* Fixed: Cannot edit transactions when in single-entry mode
* Fixed: Transaction type button sometimes hidden in single-entry mode
* Fixed: Problems saving new transactions from templates

Version 1.4.2 *(2014-08-30)*
----------------------------
* Fixed: Newly added transactions cannot be exported

Version 1.4.1 *(2014-08-25)*
----------------------------
* Fixed: Transaction edits not saved
* Fixed: Crash during import due to template transactions
* Fixed: Cursors potentially left unclosed
* Fixed: Fatal crash when error occurs in importing/exporting transaction (instead of displaying error message)
* Fixed: Editing a transfer transaction does not edit other side of the transaction
* Removed progress dialog from database migration (seems to be cause of some crashes)
* Updated German translation

Version 1.4.0 *(2014-08-15)*
----------------------------
Since transactions are at the core of this app, this release touches almost all subsystems and is therefore huge.
Transactions are now composed of multiple splits, which belong to different accounts and are no longer bound to accounts,
nor is the money amount bound to the transaction itself.
Splits store the amounts as absolute values and then separately the kind - CREDIT/DEBIT - of the split.

* Feature: Introduces multiple splits per transaction
* Feature: Introduced a new Split editor for the creation and editing of splits
* Feature: Use account specific labels for CREDIT/DEBIT instead of just generic "debit" and "credit"
* Feature: Import GnuCash XML files - accounts and transactions only (experimental)
* Feature: Back up transactions in an XML format (similar to GnuCash XML) called .gnca (Gnucash Android)
* Feature: Option for saving opening balances before deleting transactions
* Improved: Updated processes for moving, creating, exporting, deleting transactions to work with splits
* Improved: Updated computation of account and transaction balances to be in line with accounting principles
* Improved: Updated color (red/green) display to match movement in the account, and not a representation of the side of the split
* Improved: Introduced new format for sending Transactions through Intents (while maintaining backwards compatibility)
* Improved: Extensive code refactoring for
    - Better modularity of transaction exports (and ease introduction of new formats),
    - Cleaner database schema and reduction of overlap and redundancies
    - Easier database migrations during future update (with reduced risk of data loss)


Version 1.3.3 *(2014-05-26)*
----------------------------
* Reversed changes in the computation of balances, back to pre-v1.3.2 mode (will be re-instated in the future)

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
